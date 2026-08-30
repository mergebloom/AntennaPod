package de.danoeh.antennapod.net.download.service.episode;

import android.content.Context;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.net.contentcrunch.ContentCrunchAutoProcessor;
import de.danoeh.antennapod.net.contentcrunch.ContentCrunchModels;
import de.danoeh.antennapod.net.contentcrunch.ContentCrunchPreferences;
import de.danoeh.antennapod.net.contentcrunch.EpisodeMatcher;

public final class ContentCrunchDownloadScheduler {
    private ContentCrunchDownloadScheduler() { }

    public static void enqueue(Context context, FeedItem item) {
        ContentCrunchAutoProcessor.dispatchFailOpen(() -> enqueueInternal(context, item));
    }

    private static void enqueueInternal(Context context, FeedItem item) {
        ContentCrunchPreferences preferences = new ContentCrunchPreferences(context);
        ContentCrunchModels.EpisodeKey key = EpisodeMatcher.from(item);
        if (!ContentCrunchAutoProcessor.isEligible(preferences.isAutoProcessDownloadsEnabled(),
                preferences.getBaseUrl(), preferences.getAccessToken(), preferences.getRefreshCookie(), key)) {
            return;
        }
        Data input = new Data.Builder()
                .putString(ContentCrunchProcessWorker.KEY_FEED_URL, key.feedUrl)
                .putString(ContentCrunchProcessWorker.KEY_GUID, key.guid)
                .putString(ContentCrunchProcessWorker.KEY_AUDIO_URL, key.audioUrl)
                .build();
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ContentCrunchProcessWorker.class)
                .setInputData(input).setConstraints(constraints).build();
        WorkManager.getInstance(context).enqueueUniqueWork(ContentCrunchAutoProcessor.uniqueWorkName(key),
                ExistingWorkPolicy.KEEP, request);
    }
}
