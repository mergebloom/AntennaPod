package de.danoeh.antennapod.net.contentcrunch;

import java.util.ArrayList;
import java.util.List;

public final class ContentCrunchSummaryFormatter {
    private ContentCrunchSummaryFormatter() { }

    public static String format(String summary) {
        if (summary == null) {
            return "";
        }
        String[] sourceLines = summary.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        List<String> lines = new ArrayList<>();
        boolean previousBlank = true;
        for (String sourceLine : sourceLines) {
            String line = sourceLine.trim();
            if (line.isEmpty()) {
                if (!previousBlank) {
                    lines.add("");
                    previousBlank = true;
                }
                continue;
            }
            boolean heading = line.matches("#{1,6}\\s+.*");
            if (heading) {
                line = line.replaceFirst("^#{1,6}\\s+", "");
                if (!lines.isEmpty() && !previousBlank) {
                    lines.add("");
                }
            } else if (line.matches("[-*+]\\s+.*")) {
                line = "• " + line.substring(2).trim();
            } else if (line.matches("\\d+\\)\\s+.*")) {
                line = line.replaceFirst("^(\\d+)\\)\\s+", "$1. ");
            }
            line = line.replaceAll("\\*\\*([^*]+)\\*\\*", "$1")
                    .replaceAll("__([^_]+)__", "$1")
                    .replaceAll("(?<!\\*)\\*([^*]+)\\*(?!\\*)", "$1")
                    .replaceAll("(?<!_)_([^_]+)_(?!_)", "$1")
                    .replaceAll("`([^`]+)`", "$1");
            lines.add(line);
            previousBlank = false;
            if (heading) {
                lines.add("");
                previousBlank = true;
            }
        }
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return String.join("\n", lines);
    }
}
