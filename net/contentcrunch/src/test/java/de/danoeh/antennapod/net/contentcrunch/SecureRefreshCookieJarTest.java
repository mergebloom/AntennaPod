package de.danoeh.antennapod.net.contentcrunch;

import java.util.Collections;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import org.junit.Test;
import static org.junit.Assert.*;

public class SecureRefreshCookieJarTest {
    private static final class MemoryStore implements SecureRefreshCookieJar.Store {
        String cookie;
        String origin;
        @Override public String getCookie() { return cookie; }
        @Override public String getOrigin() { return origin; }
        @Override public void save(String value, String valueOrigin) { cookie = value; origin = valueOrigin; }
    }

    @Test public void returnsCookieOnlyToExactOrigin() {
        MemoryStore store = new MemoryStore();
        SecureRefreshCookieJar jar = new SecureRefreshCookieJar(store);
        HttpUrl origin = HttpUrl.get("https://example.com/");
        Cookie cookie = new Cookie.Builder().name("refreshToken").value("secret").hostOnlyDomain("example.com")
                .path("/").secure().expiresAt(System.currentTimeMillis() + 60000).build();
        jar.saveFromResponse(origin, Collections.singletonList(cookie));
        assertEquals(1, jar.loadForRequest(HttpUrl.get("https://example.com/api")).size());
        assertTrue(jar.loadForRequest(HttpUrl.get("https://example.com:8443/api")).isEmpty());
        assertTrue(jar.loadForRequest(HttpUrl.get("https://sub.example.com/api")).isEmpty());
    }

    @Test public void rejectsExpiredCookie() {
        MemoryStore store = new MemoryStore();
        store.origin = "https://example.com:443";
        store.cookie = "refreshToken=secret; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/; Secure";
        assertTrue(new SecureRefreshCookieJar(store).loadForRequest(HttpUrl.get("https://example.com/")).isEmpty());
    }
}
