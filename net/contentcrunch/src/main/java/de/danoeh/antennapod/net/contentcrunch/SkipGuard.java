package de.danoeh.antennapod.net.contentcrunch;

public final class SkipGuard {
    public static final long DURATION_MS = 3000;
    private long skippedEnd = -1;
    private long guardedUntil = -1;

    public void record(long segmentEnd, long nowMs) {
        skippedEnd = segmentEnd;
        guardedUntil = nowMs + DURATION_MS;
    }

    public boolean suppresses(long segmentEnd, long nowMs) {
        return nowMs < guardedUntil && Math.abs(segmentEnd - skippedEnd) <= SkipDecision.TOLERANCE_MS;
    }

    public void clear() {
        skippedEnd = -1;
        guardedUntil = -1;
    }
}
