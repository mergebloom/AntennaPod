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
import java.util.concurrent.TimeUnit;


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

    public ContentCrunchModels.EpisodeResult status(String requestId) throws IOException {
        if (requestId == null || requestId.isEmpty()) { throw new IOException("Content Crunch request ID is missing"); }
        return parseEpisode(executeGet("/api/v1/mobile/content-crunch/requests/" + requestId, true));
    }

    public List<ContentCrunchModels.EpisodeKey> availability(List<ContentCrunchModels.EpisodeKey> keys)
            throws IOException {
        if (keys == null || keys.isEmpty()) { return new ArrayList<>(); }
        return parseAvailability(execute("/api/v1/mobile/content-crunch/availability",
                availabilityBody(keys, preferences.getSummaryConfig()), true, true));
    }

    public ContentCrunchModels.EpisodeResult process(ContentCrunchModels.EpisodeKey key) throws IOException {
        return process(key, preferences.getSummaryConfig(), false);
    }

    public ContentCrunchModels.EpisodeResult process(ContentCrunchModels.EpisodeKey key, SummaryConfig config,
            boolean stream) throws IOException {
        JSONObject body = episodeBody(key);
        try { config.applyTo(body, stream); } catch (JSONException e) { throw new IOException(e); }
        return parseEpisode(execute("/api/v1/mobile/content-crunch/episodes/process", body, true, true));
    }

    public ContentCrunchModels.EpisodeResult processAndPoll(ContentCrunchModels.EpisodeKey key) throws IOException {
        return processAndPoll(key, preferences.getSummaryConfig());
    }

    public ContentCrunchModels.EpisodeResult processAndPoll(ContentCrunchModels.EpisodeKey key, SummaryConfig config)
            throws IOException {
        requireValid(key);
        ContentCrunchModels.EpisodeResult initial = process(key, config, true);
        return ContentCrunchPoller.poll(initial,
                () -> initial.requestId == null ? status(key) : status(initial.requestId), Thread::sleep);
    }

    public ContentCrunchModels.EpisodeResult processAndObserve(ContentCrunchModels.EpisodeKey key, SummaryConfig config,
            ContentCrunchSseParser.Listener listener) throws IOException {
        ContentCrunchModels.EpisodeResult initial = process(key, config, true);
        if (initial.eventsUrl != null && !initial.eventsUrl.isEmpty() && !ContentCrunchPoller.isTerminal(initial)) {
            try { consumeEvents(initial.eventsUrl, listener); } catch (IOException ignored) { }
        }
        ContentCrunchModels.EpisodeResult snapshot = initial.requestId == null ? status(key) : status(initial.requestId);
        return ContentCrunchPoller.poll(snapshot,
                () -> initial.requestId == null ? status(key) : status(initial.requestId), Thread::sleep);
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

    private JSONObject executeGet(String path, boolean retry) throws IOException {
        Request request = authenticatedRequest(getBaseUrl().newBuilder()
                .addEncodedPathSegments(path.substring(1)).build()).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 401 && retry && refresh()) { return executeGet(path, false); }
            return responseJson(response);
        }
    }

    private void consumeEvents(String eventsUrl, ContentCrunchSseParser.Listener listener) throws IOException {
        HttpUrl url = eventsUrl.startsWith("http") ? HttpUrl.get(eventsUrl)
                : getBaseUrl().newBuilder().addEncodedPathSegments(eventsUrl.startsWith("/")
                        ? eventsUrl.substring(1) : eventsUrl).build();
        Request request = authenticatedRequest(url).header("Accept", "text/event-stream").get().build();
        OkHttpClient streamClient = client.newBuilder().readTimeout(45, TimeUnit.SECONDS).build();
        try (Response response = streamClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Content Crunch stream failed (" + response.code() + ")");
            }
            ContentCrunchSseParser.parse(response.body().source(), listener);
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

    static JSONObject availabilityBody(List<ContentCrunchModels.EpisodeKey> keys, SummaryConfig config)
            throws IOException {
        JSONArray episodes = new JSONArray();
        for (ContentCrunchModels.EpisodeKey key : keys) {
            if (!EpisodeMatcher.isValid(key)) { continue; }
            JSONObject episode = new JSONObject();
            try {
                episode.put("feedUrl", key.feedUrl);
                if (key.guid != null && !key.guid.isEmpty()) { episode.put("guid", key.guid); }
                if (key.audioUrl != null && !key.audioUrl.isEmpty()) { episode.put("audioUrl", key.audioUrl); }
                episodes.put(episode);
            } catch (JSONException e) { throw new IOException(e); }
        }
        JSONObject body = new JSONObject();
        try {
            body.put("episodes", episodes);
            config.applyTo(body, false);
            body.remove("stream");
            return body;
        } catch (JSONException e) { throw new IOException(e); }
    }

    static List<ContentCrunchModels.EpisodeKey> parseAvailability(JSONObject root) {
        JSONObject data = root.optJSONObject("data");
        if (data == null) { data = root; }
        JSONArray available = data.optJSONArray("episodes");
        List<ContentCrunchModels.EpisodeKey> keys = new ArrayList<>();
        if (available == null) { return keys; }
        for (int i = 0; i < available.length(); i++) {
            JSONObject episode = available.optJSONObject(i);
            if (episode != null && episode.optBoolean("available", false)) {
                ContentCrunchModels.EpisodeKey key = new ContentCrunchModels.EpisodeKey(
                        episode.optString("feedUrl", null), episode.optString("guid", null),
                        episode.optString("audioUrl", null));
                if (EpisodeMatcher.isValid(key)) { keys.add(key); }
            }
        }
        return keys;
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
        JSONObject config = data.optJSONObject("summaryConfig");
        if (config == null) { config = data.optJSONObject("config"); }
        if (config == null && summary != null) { config = summary.optJSONObject("config"); }
        return new ContentCrunchModels.EpisodeResult(data.optString("status", "unknown"),
                summary == null ? data.optString("partialSummary", "") : summary.optString("text", ""), segments,
                data.optString("requestId", data.optString("jobId", null)), data.optString("eventsUrl", null),
                data.optString("stage", null), data.has("progress") ? data.optInt("progress", -1) : -1,
                config == null ? SummaryConfig.DEFAULT_SIZE
                        : config.optInt("sizeWords", config.optInt("summarySize", SummaryConfig.DEFAULT_SIZE)),
                config == null ? SummaryConfig.DEFAULT_STYLE
                        : config.optString("style", config.optString("summaryStyle", SummaryConfig.DEFAULT_STYLE)));
    }
}
