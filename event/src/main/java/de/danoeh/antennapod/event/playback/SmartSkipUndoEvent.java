package de.danoeh.antennapod.event.playback;

public class SmartSkipUndoEvent {
    public final long mediaId;
    public final long startPosition;
    public final long endPosition;

    public SmartSkipUndoEvent(long mediaId, long startPosition, long endPosition) {
        this.mediaId = mediaId;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    public SmartSkipUndoEvent(SmartSkipEvent event) {
        this(event.mediaId, event.getUndoPosition(), event.endPosition);
    }
}
