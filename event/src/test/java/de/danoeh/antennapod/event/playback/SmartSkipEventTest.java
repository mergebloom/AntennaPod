package de.danoeh.antennapod.event.playback;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SmartSkipEventTest {
    @Test
    public void carriesReasonAndUndoTarget() {
        SmartSkipEvent event = new SmartSkipEvent(42, 1000, 5000, "sponsor", "Sponsor message");
        SmartSkipUndoEvent undo = new SmartSkipUndoEvent(event);

        assertEquals(42, event.mediaId);
        assertEquals("sponsor", event.category);
        assertEquals("Sponsor message", event.label);
        assertEquals(1000, event.getUndoPosition());
        assertEquals(42, undo.mediaId);
        assertEquals(1000, undo.startPosition);
        assertEquals(5000, undo.endPosition);
    }
}
