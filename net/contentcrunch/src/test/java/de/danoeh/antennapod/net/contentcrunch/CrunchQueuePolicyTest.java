package de.danoeh.antennapod.net.contentcrunch;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.junit.Test;

public class CrunchQueuePolicyTest {
    @Test public void hidesDismissedUnlessRequested() {
        assertEquals(Arrays.asList(1L, 3L), CrunchQueuePolicy.visibleIds(Arrays.asList(1L, 2L, 3L),
                new HashSet<>(Collections.singletonList(2L)), false));
        assertEquals(Arrays.asList(1L, 2L, 3L), CrunchQueuePolicy.visibleIds(Arrays.asList(1L, 2L, 3L),
                new HashSet<>(Collections.singletonList(2L)), true));
    }

    @Test public void derivesProcessingReadyAndFailedStates() {
        assertEquals("not_processed", CrunchQueuePolicy.state(null, false));
        assertEquals("waiting_transcript", CrunchQueuePolicy.state(null, true));
        assertEquals("ready", CrunchQueuePolicy.state(
                new ContentCrunchModels.EpisodeResult("completed", "Summary", null), false));
        assertEquals("failed", CrunchQueuePolicy.state(
                new ContentCrunchModels.EpisodeResult("failed", null, null), false));
        assertEquals("generating_skip_analysis", CrunchQueuePolicy.state(
                new ContentCrunchModels.EpisodeResult("ANALYZING_SKIP_SEGMENTS", null, null), false));
        assertEquals("generating_summary", CrunchQueuePolicy.state(
                new ContentCrunchModels.EpisodeResult("PROCESSING", null, null), false));
    }

    @Test public void createsBoundedReadableExcerpt() {
        assertEquals("One two…", CrunchQueuePolicy.excerpt(" One   two three ", 8));
        assertEquals("", CrunchQueuePolicy.excerpt(null, 20));
    }
}
