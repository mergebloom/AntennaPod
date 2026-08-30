package de.danoeh.antennapod.ui.preferences.screen;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.SwitchPreferenceCompat;
import de.danoeh.antennapod.net.contentcrunch.ContentCrunchClient;
import de.danoeh.antennapod.net.contentcrunch.ContentCrunchPreferences;
import de.danoeh.antennapod.ui.preferences.R;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ContentCrunchPreferencesFragment extends AnimatedPreferenceFragment {
    private ContentCrunchPreferences preferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_content_crunch);
        preferences = new ContentCrunchPreferences(requireContext());
        EditTextPreference baseUrl = findPreference("contentCrunchBaseUrl");
        baseUrl.setText(preferences.getBaseUrl());
        baseUrl.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
        baseUrl.setOnPreferenceChangeListener((preference, value) -> {
            try {
                preferences.setBaseUrl(value.toString());
            } catch (IllegalArgumentException e) {
                Toast.makeText(requireContext(), R.string.content_crunch_https_required, Toast.LENGTH_LONG).show();
                return false;
            }
            return true;
        });
        findPreference("contentCrunchLogin").setOnPreferenceClickListener(value -> { showLogin(); return true; });
        findPreference("contentCrunchLogout").setOnPreferenceClickListener(value -> {
            preferences.logout();
            Toast.makeText(requireContext(), R.string.content_crunch_logged_out, Toast.LENGTH_SHORT).show();
            return true;
        });
        SwitchPreferenceCompat smartSkip = findPreference("contentCrunchSmartSkip");
        smartSkip.setChecked(preferences.isSmartSkipEnabled());
        smartSkip.setOnPreferenceChangeListener((preference, value) -> {
            preferences.setSmartSkipEnabled((Boolean) value); return true;
        });
        SwitchPreferenceCompat autoProcessDownloads = findPreference("contentCrunchAutoProcessDownloads");
        autoProcessDownloads.setChecked(preferences.isAutoProcessDownloadsEnabled());
        autoProcessDownloads.setOnPreferenceChangeListener((preference, value) -> {
            preferences.setAutoProcessDownloadsEnabled((Boolean) value); return true;
        });
        bindCategory("contentCrunchCategorySponsor", "sponsor");
        bindCategory("contentCrunchCategorySelfPromotion", "self_promotion");
        bindCategory("contentCrunchCategoryIntro", "intro");
        bindCategory("contentCrunchCategoryOutro", "outro");
        bindCategory("contentCrunchCategoryInteraction", "interaction_reminder");
        bindCategory("contentCrunchCategoryPreviewRecap", "preview_recap");
        bindCategory("contentCrunchCategoryFiller", "filler");
    }

    private void bindCategory(String key, String category) {
        SwitchPreferenceCompat preference = findPreference(key);
        preference.setChecked(preferences.isCategoryEnabled(category));
        preference.setOnPreferenceChangeListener((ignored, value) -> {
            preferences.setCategoryEnabled(category, (Boolean) value); return true;
        });
    }

    private void showLogin() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, 0, padding, 0);
        EditText email = new EditText(requireContext());
        email.setHint(R.string.content_crunch_email);
        email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText password = new EditText(requireContext());
        password.setHint(R.string.content_crunch_password);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(email); layout.addView(password);
        new AlertDialog.Builder(requireContext()).setTitle(R.string.content_crunch_login).setView(layout)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.content_crunch_login, (dialog, which) ->
                    Completable.fromAction(() -> ContentCrunchClient.get(requireContext())
                            .login(email.getText().toString(), password.getText().toString()))
                        .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> Toast.makeText(requireContext(), R.string.content_crunch_login_success,
                                Toast.LENGTH_SHORT).show(), error -> Toast.makeText(requireContext(),
                                R.string.content_crunch_login_failed, Toast.LENGTH_LONG).show())).show();
    }
}
