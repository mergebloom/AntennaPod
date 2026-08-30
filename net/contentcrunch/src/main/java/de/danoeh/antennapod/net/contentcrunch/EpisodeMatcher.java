package de.danoeh.antennapod.net.contentcrunch;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import java.util.Objects;

public final class EpisodeMatcher {
    private EpisodeMatcher() { }

    public static ContentCrunchModels.EpisodeKey from(FeedItem item) {
        FeedMedia media = item == null ? null : item.getMedia();
        return new ContentCrunchModels.EpisodeKey(item == null || item.getFeed() == null ? null
                : item.getFeed().getDownloadUrl(), item == null ? null : item.getItemIdentifier(),
                media == null ? null : media.getDownloadUrl());
    }

    public static boolean matches(ContentCrunchModels.EpisodeKey left, ContentCrunchModels.EpisodeKey right) {
        if (left == null || right == null || !Objects.equals(left.feedUrl, right.feedUrl)) {
            return false;
        }
        return nonEmptyEqual(left.guid, right.guid) || nonEmptyEqual(left.audioUrl, right.audioUrl);
    }

    public static boolean isValid(ContentCrunchModels.EpisodeKey key) {
        return key != null && key.feedUrl != null && !key.feedUrl.isEmpty()
                && ((key.guid != null && !key.guid.isEmpty()) || (key.audioUrl != null && !key.audioUrl.isEmpty()));
    }

    private static boolean nonEmptyEqual(String left, String right) {
        return left != null && !left.isEmpty() && Objects.equals(left, right);
    }
}
