package de.danoeh.antennapod.net.contentcrunch;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.junit.Test;
import static org.junit.Assert.*;

public class SkipDecisionTest {
    private final ContentCrunchModels.SkipSegment sponsor =
            new ContentCrunchModels.SkipSegment(1000, 5000, "sponsor", 0.9, "Sponsor");
    @Test public void skipsEnabledCategoryInsideSegment() {
        assertSame(sponsor, SkipDecision.find(1500, -1, Collections.singletonList(sponsor),
                new HashSet<>(Collections.singletonList("sponsor"))));
    }
    @Test public void doesNotSkipDisabledCategory() {
        assertNull(SkipDecision.find(1500, -1, Collections.singletonList(sponsor), Collections.emptySet()));
    }
    @Test public void suppressesLoopNearPreviousTarget() {
        assertNull(SkipDecision.find(1500, 5000, Arrays.asList(sponsor),
                new HashSet<>(Collections.singletonList("sponsor"))));
    }
}
