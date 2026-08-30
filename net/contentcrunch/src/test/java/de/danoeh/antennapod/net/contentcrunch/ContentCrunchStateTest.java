package de.danoeh.antennapod.net.contentcrunch;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class ContentCrunchStateTest {
    private static ContentCrunchModels.EpisodeResult result(String status) {
        return new ContentCrunchModels.EpisodeResult(status, status, Collections.emptyList());
    }

    @Test public void pollingStopsAtCompleted() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ContentCrunchModels.EpisodeResult value = ContentCrunchPoller.poll(result("PROCESSING"),
                () -> calls.incrementAndGet() == 2 ? result("COMPLETED") : result("PROCESSING"), delay -> { });
        assertEquals("COMPLETED", value.status);
        assertEquals(2, calls.get());
    }

    @Test public void pollingIsBoundedAndReturnsNonterminalWithoutCachingIt() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ContentCrunchModels.EpisodeResult value = ContentCrunchPoller.poll(result("PROCESSING"),
                () -> { calls.incrementAndGet(); return result("PROCESSING"); }, delay -> { });
        assertEquals(ContentCrunchPoller.MAX_ATTEMPTS, calls.get());
        ContentCrunchModels.EpisodeKey key = new ContentCrunchModels.EpisodeKey("feed", "guid", null);
        ContentCrunchCache.clear();
        ContentCrunchCache.put(key, value);
        assertNull(ContentCrunchCache.get(key));
    }

    @Test public void failedIsTerminalAndNotCached() throws Exception {
        ContentCrunchModels.EpisodeResult failed = ContentCrunchPoller.poll(result("FAILED"),
                () -> { fail(); return null; }, delay -> { });
        assertTrue(ContentCrunchPoller.isFailed(failed));
        ContentCrunchModels.EpisodeKey key = new ContentCrunchModels.EpisodeKey("feed", "guid", null);
        ContentCrunchCache.clear();
        ContentCrunchCache.put(key, failed);
        assertNull(ContentCrunchCache.get(key));
    }

    @Test public void completedResultIsCached() {
        ContentCrunchModels.EpisodeKey key = new ContentCrunchModels.EpisodeKey("feed", "guid", null);
        ContentCrunchCache.clear();
        ContentCrunchCache.put(key, result("COMPLETED"));
        assertNotNull(ContentCrunchCache.get(key));
    }

    @Test public void invalidEpisodeIdentityIsRejected() {
        assertFalse(EpisodeMatcher.isValid(new ContentCrunchModels.EpisodeKey(null, "guid", "audio")));
        assertFalse(EpisodeMatcher.isValid(new ContentCrunchModels.EpisodeKey("feed", null, null)));
        assertTrue(EpisodeMatcher.isValid(new ContentCrunchModels.EpisodeKey("feed", "guid", null)));
    }
}
