package de.danoeh.antennapod.net.contentcrunch;

import org.json.JSONObject;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class ContentCrunchClientTest {
    @Test
    public void parsesMobileApiResultDtoAndConvertsSecondsToMilliseconds() throws Exception {
        JSONObject response = new JSONObject("{\"success\":true,\"data\":{\"status\":\"COMPLETED\","
                + "\"summary\":{\"text\":\"Concise summary\",\"createdAt\":\"2026-01-01T00:00:00Z\"},"
                + "\"segments\":[{\"start\":1.25,\"end\":5.75,\"category\":\"interaction_reminder\","
                + "\"confidence\":0.9,\"label\":\"Subscribe\"}]}} ");

        ContentCrunchModels.EpisodeResult result = ContentCrunchClient.parseEpisode(response);

        assertEquals("COMPLETED", result.status);
        assertEquals("Concise summary", result.summary);
        assertEquals(1, result.skipSegments.size());
        assertEquals(1250, result.skipSegments.get(0).startTime);
        assertEquals(5750, result.skipSegments.get(0).endTime);
        assertEquals("interaction_reminder", result.skipSegments.get(0).category);
    }

    @Test
    public void parsesPendingResultWithoutSummaryOrSegments() throws Exception {
        ContentCrunchModels.EpisodeResult result = ContentCrunchClient.parseEpisode(
                new JSONObject("{\"data\":{\"status\":\"PROCESSING\",\"summary\":null,\"segments\":[]}}"));

        assertEquals("PROCESSING", result.status);
        assertEquals("", result.summary);
        assertTrue(result.skipSegments.isEmpty());
    }

    @Test
    public void parsesDurableRequestAndSelectedConfiguration() throws Exception {
        ContentCrunchModels.EpisodeResult result = ContentCrunchClient.parseEpisode(new JSONObject("{\"data\":{"
                + "\"requestId\":\"request-1\",\"eventsUrl\":\"/requests/request-1/events\","
                + "\"status\":\"PROCESSING\",\"stage\":\"summarizing\",\"progress\":40,"
                + "\"partialSummary\":\"Partial\",\"summaryConfig\":{\"sizeWords\":600,"
                + "\"style\":\"study guide\"}}}"));
        assertEquals("request-1", result.requestId);
        assertEquals("/requests/request-1/events", result.eventsUrl);
        assertEquals("summarizing", result.stage);
        assertEquals(40, result.progress);
        assertEquals("Partial", result.summary);
        assertEquals(600, result.summarySize);
        assertEquals("study guide", result.summaryStyle);
    }

    @Test
    public void serializesNestedSummaryConfiguration() throws Exception {
        JSONObject body = new SummaryConfig(150, "bullet points").applyTo(new JSONObject(), true);
        assertEquals(150, body.getJSONObject("summaryConfig").getInt("sizeWords"));
        assertEquals("bullet points", body.getJSONObject("summaryConfig").getString("style"));
        assertTrue(body.getBoolean("stream"));
    }

    @Test
    public void serializesAvailabilityBatchAndConfiguration() throws Exception {
        List<ContentCrunchModels.EpisodeKey> keys = Arrays.asList(
                new ContentCrunchModels.EpisodeKey("https://feed/one", "guid-1", null),
                new ContentCrunchModels.EpisodeKey("https://feed/two", null, "https://audio/two"));

        JSONObject body = ContentCrunchClient.availabilityBody(keys, new SummaryConfig(600, "study guide"));

        assertEquals(2, body.getJSONArray("episodes").length());
        assertEquals("guid-1", body.getJSONArray("episodes").getJSONObject(0).getString("guid"));
        assertFalse(body.getJSONArray("episodes").getJSONObject(0).has("audioUrl"));
        assertEquals(600, body.getJSONObject("summaryConfig").getInt("sizeWords"));
        assertEquals("study guide", body.getJSONObject("summaryConfig").getString("style"));
    }

    @Test
    public void parsesAvailableEpisodeKeysFromWrappedResponse() throws Exception {
        JSONObject response = new JSONObject("{\"data\":{\"episodes\":["
                + "{\"feedUrl\":\"https://feed/one\",\"guid\":\"guid-1\",\"available\":true},"
                + "{\"feedUrl\":\"https://feed/two\",\"audioUrl\":\"https://audio/two\",\"available\":true},"
                + "{\"feedUrl\":\"https://feed/three\",\"guid\":\"guid-3\",\"available\":false}]}} ");

        List<ContentCrunchModels.EpisodeKey> available = ContentCrunchClient.parseAvailability(response);

        assertEquals(2, available.size());
        assertEquals("guid-1", available.get(0).guid);
        assertEquals("https://audio/two", available.get(1).audioUrl);
    }
}
