package de.danoeh.antennapod.net.contentcrunch;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SummaryConfigTest {
    @Test public void defaultsInvalidSelections() {
        SummaryConfig config = new SummaryConfig(42, "unknown");
        assertEquals(SummaryConfig.DEFAULT_SIZE, config.sizeWords);
        assertEquals(SummaryConfig.DEFAULT_STYLE, config.style);
    }

    @Test public void serializesSelectedConfigAndStreamFlag() throws Exception {
        JSONObject body = new SummaryConfig(600, "study guide").applyTo(new JSONObject(), true);
        assertEquals(600, body.getJSONObject("summaryConfig").getInt("sizeWords"));
        assertEquals("study guide", body.getJSONObject("summaryConfig").getString("style"));
        assertTrue(body.getBoolean("stream"));
        assertFalse(body.has("summarySize"));
    }
}
