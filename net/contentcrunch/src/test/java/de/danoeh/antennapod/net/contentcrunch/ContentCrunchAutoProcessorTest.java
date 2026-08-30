package de.danoeh.antennapod.net.contentcrunch;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class ContentCrunchAutoProcessorTest {
    private static final ContentCrunchModels.EpisodeKey VALID =
            new ContentCrunchModels.EpisodeKey("https://feed.example/rss", "guid", "https://cdn.example/e.mp3");

    @Test
    public void eligibleOnlyWhenEnabledConfiguredAuthenticatedAndValid() {
        assertTrue(ContentCrunchAutoProcessor.isEligible(true, "https://crunch.example", "token", null, VALID));
        assertTrue(ContentCrunchAutoProcessor.isEligible(true, "https://crunch.example", null, "cookie", VALID));
        assertFalse(ContentCrunchAutoProcessor.isEligible(false, "https://crunch.example", "token", null, VALID));
        assertFalse(ContentCrunchAutoProcessor.isEligible(true, "", "token", null, VALID));
        assertFalse(ContentCrunchAutoProcessor.isEligible(true, "https://crunch.example", null, null, VALID));
        assertFalse(ContentCrunchAutoProcessor.isEligible(true, "https://crunch.example", "token", null,
                new ContentCrunchModels.EpisodeKey(null, "guid", "audio")));
    }

    @Test
    public void submitsOnlyWhenLookupIsNeitherTerminalNorInFlight() {
        assertTrue(ContentCrunchAutoProcessor.shouldSubmit(null));
        assertTrue(ContentCrunchAutoProcessor.shouldSubmit(result("unknown")));
        assertTrue(ContentCrunchAutoProcessor.shouldSubmit(result("NOT_FOUND")));
        assertFalse(ContentCrunchAutoProcessor.shouldSubmit(result("PENDING")));
        assertFalse(ContentCrunchAutoProcessor.shouldSubmit(result("PROCESSING")));
        assertFalse(ContentCrunchAutoProcessor.shouldSubmit(result("COMPLETED")));
        assertFalse(ContentCrunchAutoProcessor.shouldSubmit(result("FAILED")));
    }

    @Test
    public void workNameIsStableForSameEpisodeAndDifferentForDifferentEpisode() {
        assertEquals(ContentCrunchAutoProcessor.uniqueWorkName(VALID),
                ContentCrunchAutoProcessor.uniqueWorkName(new ContentCrunchModels.EpisodeKey(
                        VALID.feedUrl, VALID.guid, "different-audio")));
        assertNotEquals(ContentCrunchAutoProcessor.uniqueWorkName(VALID),
                ContentCrunchAutoProcessor.uniqueWorkName(new ContentCrunchModels.EpisodeKey(
                        VALID.feedUrl, "other-guid", VALID.audioUrl)));
    }

    @Test
    public void dispatchFailureNeverEscapesDownloadCompletionPath() {
        AtomicInteger calls = new AtomicInteger();
        ContentCrunchAutoProcessor.dispatchFailOpen(() -> {
            calls.incrementAndGet();
            throw new IllegalStateException("WorkManager unavailable");
        });
        assertEquals(1, calls.get());
    }

    private static ContentCrunchModels.EpisodeResult result(String status) {
        return new ContentCrunchModels.EpisodeResult(status, "", null);
    }
}
