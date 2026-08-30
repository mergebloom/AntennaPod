package de.danoeh.antennapod.net.contentcrunch;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ContentCrunchPreferences {
    public static final String[] CATEGORIES = {"sponsor", "self_promotion", "intro", "outro",
            "interaction_reminder", "preview_recap", "filler"};
    private static final String FILE = "content_crunch_secure";
    private final SharedPreferences preferences;

    public ContentCrunchPreferences(Context context) {
        try {
            MasterKey key = new MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
            preferences = EncryptedSharedPreferences.create(context, FILE, key,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Secure Content Crunch storage unavailable", e);
        }
    }

    public String getBaseUrl() { return preferences.getString("base_url", ""); }
    public void setBaseUrl(String value) {
        final String canonical;
        try {
            canonical = ContentCrunchOrigin.canonicalOrigin(value);
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        SharedPreferences.Editor editor = preferences.edit().putString("base_url", canonical);
        if (!canonical.equals(getBaseUrl())) {
            editor.remove("access_token").remove("refresh_cookie").remove("refresh_cookie_origin");
            ContentCrunchCache.clear();
        }
        editor.apply();
    }
    public String getAccessToken() { return preferences.getString("access_token", null); }
    public void setAccessToken(String value) { preferences.edit().putString("access_token", value).apply(); }
    public String getRefreshCookie() { return preferences.getString("refresh_cookie", null); }
    public void setRefreshCookie(String value) { preferences.edit().putString("refresh_cookie", value).apply(); }
    public String getRefreshCookieOrigin() { return preferences.getString("refresh_cookie_origin", null); }
    public void setRefreshCookieOrigin(String value) {
        preferences.edit().putString("refresh_cookie_origin", value).apply();
    }
    public boolean isSmartSkipEnabled() { return preferences.getBoolean("smart_skip", false); }
    public void setSmartSkipEnabled(boolean value) { preferences.edit().putBoolean("smart_skip", value).apply(); }
    public boolean isCategoryEnabled(String category) { return preferences.getBoolean("category_" + category, true); }
    public void setCategoryEnabled(String category, boolean value) {
        preferences.edit().putBoolean("category_" + category, value).apply();
    }
    public Set<String> enabledCategories() {
        Set<String> result = new HashSet<>();
        Arrays.stream(CATEGORIES).filter(this::isCategoryEnabled).forEach(result::add);
        return result;
    }
    public void logout() {
        preferences.edit().remove("access_token").remove("refresh_cookie").remove("refresh_cookie_origin").apply();
    }
}
