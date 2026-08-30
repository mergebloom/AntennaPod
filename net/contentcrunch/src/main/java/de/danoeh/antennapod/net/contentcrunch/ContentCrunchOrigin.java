package de.danoeh.antennapod.net.contentcrunch;

import java.io.IOException;
import okhttp3.HttpUrl;

public final class ContentCrunchOrigin {
    private ContentCrunchOrigin() { }

    public static HttpUrl canonicalBaseUrl(String value) throws IOException {
        HttpUrl url = HttpUrl.parse(value == null ? "" : value.trim());
        if (url == null || !url.isHttps() || url.host().isEmpty() || !url.username().isEmpty()
                || !url.password().isEmpty() || url.query() != null || url.fragment() != null
                || !"/".equals(url.encodedPath())) {
            throw new IOException("Content Crunch requires an HTTPS origin without a path, user info, query, or fragment");
        }
        return new HttpUrl.Builder().scheme(url.scheme()).host(url.host()).port(url.port()).build();
    }

    public static String canonicalOrigin(String value) throws IOException {
        return canonicalBaseUrl(value).toString();
    }

    public static boolean isSameOrigin(HttpUrl left, HttpUrl right) {
        return left.scheme().equals(right.scheme()) && left.host().equals(right.host()) && left.port() == right.port();
    }
}
