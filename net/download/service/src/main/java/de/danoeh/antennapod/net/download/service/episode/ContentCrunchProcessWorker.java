package de.danoeh.antennapod.net.download.service.episode;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import de.danoeh.antennapod.net.contentcrunch.ContentCrunchAutoProcessor;
import de.danoeh.antennapod.net.contentcrunch.ContentCrunchCache;
import de.danoeh.antennapod.net.contentcrunch.ContentCrunchClient;
import de.danoeh.antennapod.net.contentcrunch.ContentCrunchModels;
import java.io.IOException;

public class ContentCrunchProcessWorker extends Worker {
    public static final String KEY_FEED_URL = "feed_url";
    public static final String KEY_GUID = "guid";
    public static final String KEY_AUDIO_URL = "audio_url";
    private static final String TAG = "ContentCrunchWorker";

    public ContentCrunchProcessWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        ContentCrunchModels.EpisodeKey key = new ContentCrunchModels.EpisodeKey(
                getInputData().getString(KEY_FEED_URL), getInputData().getString(KEY_GUID),
                getInputData().getString(KEY_AUDIO_URL));
        try {
            ContentCrunchClient client = ContentCrunchClient.get(getApplicationContext());
            ContentCrunchModels.EpisodeResult existing = client.lookup(key);
            if (!ContentCrunchAutoProcessor.shouldSubmit(existing)) {
                ContentCrunchCache.put(key, existing);
                return Result.success();
            }
            ContentCrunchModels.EpisodeResult submitted = client.process(key);
            ContentCrunchCache.put(key, submitted);
        } catch (IOException e) {
            Log.w(TAG, "Automatic processing attempt failed", e);
            return getRunAttemptCount() < 4 ? Result.retry() : Result.failure();
        } catch (RuntimeException e) {
            Log.w(TAG, "Automatic processing is permanently ineligible", e);
            return Result.failure();
        }
        return Result.success();
    }
}
