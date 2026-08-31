package de.danoeh.antennapod.net.contentcrunch;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Local-only state for hiding episodes from the summary queue. */
public final class CrunchQueueStore {
    private static final String PREFS = "content_crunch_queue";
    private static final String DISMISSED = "dismissed_episode_ids";
    private final SharedPreferences preferences;

    public CrunchQueueStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public Set<Long> getDismissedIds() {
        Set<String> values = preferences.getStringSet(DISMISSED, Collections.emptySet());
        Set<Long> result = new HashSet<>();
        for (String value : values) {
            try { result.add(Long.parseLong(value)); } catch (NumberFormatException ignored) { }
        }
        return result;
    }

    public void dismiss(long episodeId) {
        Set<String> values = new HashSet<>(preferences.getStringSet(DISMISSED, Collections.emptySet()));
        values.add(Long.toString(episodeId));
        preferences.edit().putStringSet(DISMISSED, values).apply();
    }

    public void reset() { preferences.edit().remove(DISMISSED).apply(); }

    public void restore(long id) {
        Set<String> values = new HashSet<>(preferences.getStringSet(DISMISSED, Collections.emptySet()));
        values.remove(Long.toString(id));
        preferences.edit().putStringSet(DISMISSED, values).apply();
    }
}
