package de.danoeh.antennapod.net.contentcrunch;

import org.junit.Test;
import static org.junit.Assert.*;

public class EpisodeMatcherTest {
    @Test public void matchesGuidWithinSameFeed() {
        assertTrue(EpisodeMatcher.matches(new ContentCrunchModels.EpisodeKey("feed", "guid", "a"),
                new ContentCrunchModels.EpisodeKey("feed", "guid", "b")));
    }
    @Test public void fallsBackToAudioUrl() {
        assertTrue(EpisodeMatcher.matches(new ContentCrunchModels.EpisodeKey("feed", null, "audio"),
                new ContentCrunchModels.EpisodeKey("feed", null, "audio")));
    }
    @Test public void neverMatchesDifferentFeeds() {
        assertFalse(EpisodeMatcher.matches(new ContentCrunchModels.EpisodeKey("one", "guid", "audio"),
                new ContentCrunchModels.EpisodeKey("two", "guid", "audio")));
    }
}
