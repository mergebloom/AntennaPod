package de.danoeh.antennapod.net.contentcrunch;

import java.util.Collections;
import java.util.List;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

final class SecureRefreshCookieJar implements CookieJar {
    interface Store {
        String getCookie();
        String getOrigin();
        void save(String cookie, String origin);
    }

    private final Store store;

    SecureRefreshCookieJar(ContentCrunchPreferences preferences) {
        this(new Store() {
            @Override public String getCookie() { return preferences.getRefreshCookie(); }
            @Override public String getOrigin() { return preferences.getRefreshCookieOrigin(); }
            @Override public void save(String cookie, String origin) {
                preferences.setRefreshCookie(cookie);
                preferences.setRefreshCookieOrigin(origin);
            }
        });
    }

    SecureRefreshCookieJar(Store store) { this.store = store; }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.name()) && cookie.secure() && cookie.matches(url)) {
                store.save(cookie.toString(), origin(url));
            }
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        String encoded = store.getCookie();
        if (encoded == null || !origin(url).equals(store.getOrigin())) { return Collections.emptyList(); }
        Cookie cookie = Cookie.parse(url, encoded);
        if (cookie == null || !cookie.secure() || cookie.expiresAt() <= System.currentTimeMillis()
                || !cookie.matches(url)) {
            return Collections.emptyList();
        }
        return Collections.singletonList(cookie);
    }

    private static String origin(HttpUrl url) {
        return url.scheme() + "://" + url.host() + ":" + url.port();
    }
}
