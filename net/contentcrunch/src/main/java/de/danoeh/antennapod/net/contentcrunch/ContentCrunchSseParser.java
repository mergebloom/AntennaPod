package de.danoeh.antennapod.net.contentcrunch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okio.BufferedSource;

public final class ContentCrunchSseParser {
    public static final class Event {
        public final String id;
        public final String type;
        public final String data;

        Event(String id, String type, String data) {
            this.id = id;
            this.type = type;
            this.data = data;
        }
    }

    public interface Listener { boolean onEvent(Event event) throws IOException; }

    private ContentCrunchSseParser() { }

    public static void parse(BufferedSource source, Listener listener) throws IOException {
        String id = null;
        String type = "message";
        List<String> data = new ArrayList<>();
        String line;
        while ((line = source.readUtf8Line()) != null) {
            if (line.isEmpty()) {
                if (!data.isEmpty()) {
                    if (!listener.onEvent(new Event(id, type, String.join("\n", data)))) { return; }
                }
                type = "message";
                data.clear();
            } else if (!line.startsWith(":")) {
                int colon = line.indexOf(':');
                String field = colon < 0 ? line : line.substring(0, colon);
                String value = colon < 0 ? "" : line.substring(colon + 1);
                if (value.startsWith(" ")) { value = value.substring(1); }
                if (field.equals("id") && !value.contains("\u0000")) { id = value; }
                else if (field.equals("event")) { type = value; }
                else if (field.equals("data")) { data.add(value); }
            }
        }
        if (!data.isEmpty()) { listener.onEvent(new Event(id, type, String.join("\n", data))); }
    }

    public static String appendDelta(String current, int offset, String delta) throws IOException {
        if (offset != current.length()) { throw new IOException("Content Crunch summary stream offset mismatch"); }
        return current + delta;
    }
}
