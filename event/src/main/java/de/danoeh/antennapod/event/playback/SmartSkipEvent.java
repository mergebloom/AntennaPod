package de.danoeh.antennapod.event.playback;

public class SmartSkipEvent {
    public final long mediaId;
    public final long startPosition;
    public final long endPosition;
    public final String category;
    public final String label;

    public SmartSkipEvent(long mediaId, long startPosition, long endPosition, String category, String label) {
        this.mediaId = mediaId;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.category = category;
        this.label = label;
    }

    public long getUndoPosition() {
        return startPosition;
    }
}
