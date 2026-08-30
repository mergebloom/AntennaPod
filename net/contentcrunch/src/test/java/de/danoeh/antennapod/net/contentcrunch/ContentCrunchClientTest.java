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
}
