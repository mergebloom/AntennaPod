package de.danoeh.antennapod.net.contentcrunch;

import android.content.Context;
import androidx.annotation.Nullable;
import de.danoeh.antennapod.net.common.AntennapodHttpClient;
import okhttp3.MediaType;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public final class ContentCrunchClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static volatile ContentCrunchClient instance;
    private final ContentCrunchPreferences preferences;
    private final OkHttpClient client;

    public static ContentCrunchClient get(Context context) {
        if (instance == null) {
            synchronized (ContentCrunchClient.class) {
                if (instance == null) {
                    Context appContext = context.getApplicationContext();
                    instance = new ContentCrunchClient(new ContentCrunchPreferences(appContext));
                }
            }
        }
        return instance;
    }

    public ContentCrunchClient(ContentCrunchPreferences preferences) {
        this.preferences = preferences;
        client = AntennapodHttpClient.newBuilder().cookieJar(new SecureRefreshCookieJar(preferences)).build();
    }

    public void login(String email, String password) throws IOException {
        JSONObject body = new JSONObject();
        try { body.put("email", email).put("password", password); } catch (JSONException e) { throw new IOException(e); }
        JSONObject json = execute("/api/v1/auth/login", body, false, false);
        String token = json.optJSONObject("data") == null ? null : json.optJSONObject("data").optString("accessToken", null);
        if (token == null) { throw new IOException("Content Crunch authentication failed"); }
        preferences.setAccessToken(token);
    }

    public ContentCrunchModels.EpisodeResult lookup(ContentCrunchModels.EpisodeKey key) throws IOException {
        return parseEpisode(executeGet("/api/v1/mobile/content-crunch/episodes/lookup", key, true));
    }

    public ContentCrunchModels.EpisodeResult status(ContentCrunchModels.EpisodeKey key) throws IOException {
        return parseEpisode(executeGet("/api/v1/mobile/content-crunch/episodes/result", key, true));
    }

    public ContentCrunchModels.EpisodeResult process(ContentCrunchModels.EpisodeKey key) throws IOException {
        return parseEpisode(execute("/api/v1/mobile/content-crunch/episodes/process", episodeBody(key), true, true));
    }

    public ContentCrunchModels.EpisodeResult processAndPoll(ContentCrunchModels.EpisodeKey key) throws IOException {
        requireValid(key);
        return ContentCrunchPoller.poll(process(key), () -> status(key), Thread::sleep);
    }

    public ContentCrunchModels.EpisodeResult lookupAndPoll(ContentCrunchModels.EpisodeKey key) throws IOException {
        requireValid(key);
        return ContentCrunchPoller.poll(lookup(key), () -> status(key), Thread::sleep);
    }

    private JSONObject executeGet(String path, ContentCrunchModels.EpisodeKey key, boolean retry) throws IOException {
        requireValid(key);
        HttpUrl baseUrl = getBaseUrl();
        HttpUrl.Builder url = baseUrl.newBuilder().addEncodedPathSegments(path.substring(1))
                .addQueryParameter("feedUrl", key.feedUrl);
        if (key.guid != null && !key.guid.isEmpty()) { url.addQueryParameter("guid", key.guid); }
        if (key.audioUrl != null && !key.audioUrl.isEmpty()) { url.addQueryParameter("audioUrl", key.audioUrl); }
        Request.Builder builder = authenticatedRequest(url.build()).get();
        try (Response response = client.newCall(builder.build()).execute()) {
            if (response.code() == 401 && retry && refresh()) { return executeGet(path, key, false); }
            return responseJson(response);
        }
    }

    private JSONObject execute(String path, JSONObject body, boolean authenticated, boolean retry) throws IOException {
        Request.Builder builder = new Request.Builder().url(getBaseUrl().newBuilder()
                .addEncodedPathSegments(path.substring(1)).build())
                .post(RequestBody.create(body.toString(), JSON));
        String token = preferences.getAccessToken();
        if (authenticated && token != null) { builder.header("Authorization", "Bearer " + token); }
        try (Response response = client.newCall(builder.build()).execute()) {
            if (response.code() == 401 && authenticated && retry && refresh()) {
                return execute(path, body, true, false);
            }
            return responseJson(response);
        }
    }

    private HttpUrl getBaseUrl() throws IOException {
        return ContentCrunchOrigin.canonicalBaseUrl(preferences.getBaseUrl());
    }

    private Request.Builder authenticatedRequest(HttpUrl url) {
        Request.Builder builder = new Request.Builder().url(url);
        String token = preferences.getAccessToken();
        if (token != null) { builder.header("Authorization", "Bearer " + token); }
        return builder;
    }

    private static JSONObject responseJson(Response response) throws IOException {
        if (!response.isSuccessful()) { throw new IOException("Content Crunch request failed (" + response.code() + ")"); }
        String text = response.body() == null ? "{}" : response.body().string();
        try { return new JSONObject(text); } catch (JSONException e) { throw new IOException("Invalid Content Crunch response"); }
    }

    private boolean refresh() throws IOException {
        JSONObject json = execute("/api/v1/auth/refresh", new JSONObject(), false, false);
        JSONObject data = json.optJSONObject("data");
        String token = data == null ? null : data.optString("accessToken", null);
        if (token == null) { preferences.logout(); return false; }
        preferences.setAccessToken(token);
        return true;
    }

    private static JSONObject episodeBody(ContentCrunchModels.EpisodeKey key) throws IOException {
        requireValid(key);
        try { return new JSONObject().put("feedUrl", key.feedUrl).put("guid", key.guid).put("audioUrl", key.audioUrl); }
        catch (JSONException e) { throw new IOException(e); }
    }

    private static void requireValid(ContentCrunchModels.EpisodeKey key) throws IOException {
        if (!EpisodeMatcher.isValid(key)) { throw new IOException("Content Crunch episode identity is incomplete"); }
    }

    static ContentCrunchModels.EpisodeResult parseEpisode(JSONObject root) {
        JSONObject data = root.optJSONObject("data");
        if (data == null) { data = root; }
        JSONArray jsonSegments = data.optJSONArray("segments");
        List<ContentCrunchModels.SkipSegment> segments = new ArrayList<>();
        if (jsonSegments != null) {
            for (int i = 0; i < jsonSegments.length(); i++) {
                JSONObject segment = jsonSegments.optJSONObject(i);
                if (segment != null) {
                    segments.add(new ContentCrunchModels.SkipSegment(Math.round(segment.optDouble("start") * 1000),
                            Math.round(segment.optDouble("end") * 1000), segment.optString("category"),
                            segment.optDouble("confidence"), segment.optString("label")));
                }
            }
        }
        JSONObject summary = data.optJSONObject("summary");
        return new ContentCrunchModels.EpisodeResult(data.optString("status", "unknown"),
                summary == null ? "" : summary.optString("text", ""), segments);
    }
}
