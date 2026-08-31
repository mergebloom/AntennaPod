package de.danoeh.antennapod.net.contentcrunch;

import org.json.JSONObject;
import org.junit.Test;
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
}
