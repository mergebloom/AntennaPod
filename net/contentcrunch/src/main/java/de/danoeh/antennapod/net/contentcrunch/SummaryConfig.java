package de.danoeh.antennapod.net.contentcrunch;

import org.json.JSONException;
import org.json.JSONObject;

public final class SummaryConfig {
    public static final int DEFAULT_SIZE = 300;
    public static final String DEFAULT_STYLE = "mece";
    public static final int[] SIZES = {150, 300, 600, 1000};
    public static final String[] STYLES = {"bullet points", "narrative paragraphs", "cheat sheet",
            "executive summary", "mece", "study guide"};

    public final int sizeWords;
    public final String style;

    public SummaryConfig(int sizeWords, String style) {
        this.sizeWords = isValidSize(sizeWords) ? sizeWords : DEFAULT_SIZE;
        this.style = isValidStyle(style) ? style : DEFAULT_STYLE;
    }

    public JSONObject applyTo(JSONObject body, boolean stream) throws JSONException {
        JSONObject config = new JSONObject().put("sizeWords", sizeWords).put("style", style);
        return body.put("summaryConfig", config).put("stream", stream);
    }

    public static boolean isValidSize(int size) {
        for (int value : SIZES) { if (value == size) { return true; } }
        return false;
    }

    public static boolean isValidStyle(String style) {
        for (String value : STYLES) { if (value.equals(style)) { return true; } }
        return false;
    }
}
