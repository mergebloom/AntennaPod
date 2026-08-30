package de.danoeh.antennapod.net.contentcrunch;

import java.io.IOException;

public final class ContentCrunchPoller {
    public static final int MAX_ATTEMPTS = 6;
    public static final long INITIAL_DELAY_MS = 500;

    interface Source {
        ContentCrunchModels.EpisodeResult get() throws IOException;
    }

    interface Sleeper {
        void sleep(long delayMs) throws InterruptedException;
    }

    private ContentCrunchPoller() { }

    static ContentCrunchModels.EpisodeResult poll(ContentCrunchModels.EpisodeResult initial, Source source,
            Sleeper sleeper) throws IOException {
        ContentCrunchModels.EpisodeResult result = initial;
        long delay = INITIAL_DELAY_MS;
        for (int attempt = 0; attempt < MAX_ATTEMPTS && !isTerminal(result); attempt++) {
            try {
                sleeper.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Content Crunch polling cancelled", e);
            }
            result = source.get();
            delay = Math.min(delay * 2, 4000);
        }
        return result;
    }

    public static boolean isCompleted(ContentCrunchModels.EpisodeResult result) {
        return result != null && "COMPLETED".equals(result.status);
    }

    public static boolean isFailed(ContentCrunchModels.EpisodeResult result) {
        return result != null && "FAILED".equals(result.status);
    }

    public static boolean isTerminal(ContentCrunchModels.EpisodeResult result) {
        return isCompleted(result) || isFailed(result);
    }
}
