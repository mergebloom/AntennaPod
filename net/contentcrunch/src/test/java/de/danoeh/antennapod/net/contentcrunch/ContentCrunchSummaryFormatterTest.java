package de.danoeh.antennapod.net.contentcrunch;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ContentCrunchSummaryFormatterTest {
    @Test
    public void preservesParagraphsHeadingsAndBulletsAsPlainText() {
        String input = "# Overview\n\nFirst paragraph.\nContinues here.\n\n- First point\n* Second point\n\n## Details\nMore detail.";

        assertEquals("Overview\n\nFirst paragraph.\nContinues here.\n\n• First point\n• Second point\n\nDetails\n\nMore detail.",
                ContentCrunchSummaryFormatter.format(input));
    }

    @Test
    public void normalizesLineEndingsWhitespaceAndExcessBlankLines() {
        String input = "  Title  \r\n\r\n\r\n  Text with space   \r\n\r\n";

        assertEquals("Title\n\nText with space", ContentCrunchSummaryFormatter.format(input));
    }

    @Test
    public void keepsNumberedListsAndDoesNotInterpretHtml() {
        String input = "1. One\n2) Two\n\n<script>alert('x')</script>";

        assertEquals("1. One\n2. Two\n\n<script>alert('x')</script>", ContentCrunchSummaryFormatter.format(input));
    }

    @Test
    public void handlesNullAndEmptyInput() {
        assertEquals("", ContentCrunchSummaryFormatter.format(null));
        assertEquals("", ContentCrunchSummaryFormatter.format(" \n "));
    }

    @Test
    public void removesInlineMarkdownArtifactsWithoutRenderingHtml() {
        assertEquals("Important, emphasis, term, code and <b>literal HTML</b>",
                ContentCrunchSummaryFormatter.format(
                        "**Important**, *emphasis*, __term__, `code` and <b>literal HTML</b>"));
    }
}
