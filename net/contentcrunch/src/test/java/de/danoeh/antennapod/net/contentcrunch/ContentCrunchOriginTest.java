package de.danoeh.antennapod.net.contentcrunch;

import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

public class ContentCrunchOriginTest {
    @Test public void canonicalizesDefaultPortAndCase() throws Exception {
        assertEquals("https://example.com/", ContentCrunchOrigin.canonicalOrigin(" HTTPS://Example.COM:443/ "));
    }

    @Test public void preservesNonDefaultPort() throws Exception {
        assertEquals("https://example.com:8443/", ContentCrunchOrigin.canonicalOrigin("https://example.com:8443"));
    }

    @Test public void rejectsUnsafeOrAmbiguousUrls() {
        assertRejected("http://example.com");
        assertRejected("https://user@example.com");
        assertRejected("https://example.com/base");
        assertRejected("https://example.com/?query=yes");
        assertRejected("https://example.com/#fragment");
    }

    private static void assertRejected(String value) {
        try {
            ContentCrunchOrigin.canonicalOrigin(value);
            fail(value);
        } catch (IOException expected) {
        }
    }
}
