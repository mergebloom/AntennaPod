package de.danoeh.antennapod.net.contentcrunch;

import java.util.ArrayList;
import java.util.List;
import okio.Buffer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ContentCrunchSseParserTest {
    @Test public void parsesCommentsMultilineDataAndFinalEvent() throws Exception {
        Buffer source = new Buffer().writeUtf8(": heartbeat\nid: 4\nevent: stage\ndata: first\ndata: second\n\n"
                + "event: completed\ndata: {}\n");
        List<ContentCrunchSseParser.Event> events = new ArrayList<>();
        ContentCrunchSseParser.parse(source, event -> { events.add(event); return true; });
        assertEquals(2, events.size());
        assertEquals("4", events.get(0).id);
        assertEquals("stage", events.get(0).type);
        assertEquals("first\nsecond", events.get(0).data);
        assertEquals("completed", events.get(1).type);
    }

    @Test public void stopsWhenListenerRejectsEvent() throws Exception {
        Buffer source = new Buffer().writeUtf8("data: one\n\ndata: two\n\n");
        List<String> events = new ArrayList<>();
        ContentCrunchSseParser.parse(source, event -> { events.add(event.data); return false; });
        assertEquals(1, events.size());
    }

    @Test public void appendsOnlyContiguousDeltas() throws Exception {
        assertEquals("hello world", ContentCrunchSseParser.appendDelta("hello ", 6, "world"));
        assertThrows(java.io.IOException.class,
                () -> ContentCrunchSseParser.appendDelta("hello", 2, "world"));
    }
}
