package de.danoeh.antennapod.net.contentcrunch;

import org.junit.Test;
import static org.junit.Assert.*;

public class SkipGuardTest {
    @Test public void guardExpiresSoManualSeekBackCanSkipAgain() {
        SkipGuard guard = new SkipGuard();
        guard.record(5000, 1000);
        assertTrue(guard.suppresses(5000, 1001));
        assertFalse(guard.suppresses(5000, 1000 + SkipGuard.DURATION_MS));
    }

    @Test public void undoGuardLastsUntilSegmentHasBeenListenedTo() {
        SkipGuard guard = new SkipGuard();
        guard.recordUndo(5000);
        assertTrue(guard.suppresses(5000, Long.MAX_VALUE - 1));
        guard.updatePosition(4999);
        assertTrue(guard.suppresses(5000, Long.MAX_VALUE - 1));
        guard.updatePosition(5000);
        assertFalse(guard.suppresses(5000, 1001));
    }

    @Test public void undoGuardDoesNotSuppressDifferentSegment() {
        SkipGuard guard = new SkipGuard();
        guard.recordUndo(5000);
        assertFalse(guard.suppresses(9000, 1001));
    }
}
