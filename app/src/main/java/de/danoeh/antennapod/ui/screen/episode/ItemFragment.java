package de.danoeh.antennapod.ui.screen.episode;

import android.content.Context;
import android.os.Bundle;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.skydoves.balloon.ArrowOrientation;
import com.skydoves.balloon.ArrowOrientationRules;
import com.skydoves.balloon.Balloon;
import com.skydoves.balloon.BalloonAnimation;
import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.actionbutton.CancelDownloadActionButton;
import de.danoeh.antennapod.actionbutton.DeleteActionButton;
import de.danoeh.antennapod.actionbutton.DownloadActionButton;
import de.danoeh.antennapod.actionbutton.ItemActionButton;
import de.danoeh.antennapod.actionbutton.MarkAsPlayedActionButton;
import de.danoeh.antennapod.actionbutton.PauseActionButton;
import de.danoeh.antennapod.actionbutton.PlayActionButton;
import de.danoeh.antennapod.actionbutton.PlayLocalActionButton;
import de.danoeh.antennapod.actionbutton.StreamActionButton;
import de.danoeh.antennapod.actionbutton.VisitWebsiteActionButton;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.databinding.FeeditemFragmentBinding;
import de.danoeh.antennapod.ui.common.ClipboardUtils;
import de.danoeh.antennapod.event.EpisodeDownloadEvent;
import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.event.FeedListUpdateEvent;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.contentcrunch.ContentCrunchCache;
import de.danoeh.antennapod.net.contentcrunch.ContentCrunchClient;
import de.danoeh.antennapod.net.contentcrunch.ContentCrunchModels;
import de.danoeh.antennapod.net.contentcrunch.ContentCrunchPoller;
import de.danoeh.antennapod.net.contentcrunch.ContentCrunchPreferences;
import de.danoeh.antennapod.net.contentcrunch.ContentCrunchSseParser;
import de.danoeh.antennapod.net.contentcrunch.ContentCrunchSummaryFormatter;
import de.danoeh.antennapod.net.contentcrunch.EpisodeMatcher;
import de.danoeh.antennapod.net.contentcrunch.SummaryConfig;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.playback.service.PlaybackController;
import de.danoeh.antennapod.playback.service.PlaybackService;
import de.danoeh.antennapod.playback.service.PlaybackStatus;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.preferences.UsageStatistics;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter;
import de.danoeh.antennapod.ui.cleaner.ShownotesCleaner;
import de.danoeh.antennapod.ui.common.Converter;
import de.danoeh.antennapod.ui.common.DateFormatter;
import de.danoeh.antennapod.ui.common.ImagePlaceholder;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.ui.episodes.ImageResourceUtils;
import de.danoeh.antennapod.ui.screen.feed.FeedItemlistFragment;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;

/**
 * Displays information about a FeedItem and actions.
 */
public class ItemFragment extends Fragment {

    private static final String TAG = "ItemFragment";
    private static final String ARG_FEEDITEM = "feeditem";

    /**
     * Creates a new instance of an ItemFragment
     *
     * @param feeditem The ID of the FeedItem to show
     * @return The ItemFragment instance
     */
    public static ItemFragment newInstance(long feeditem) {
        ItemFragment fragment = new ItemFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_FEEDITEM, feeditem);
        fragment.setArguments(args);
        return fragment;
    }

    private boolean itemsLoaded = false;
    private long itemId;
    private FeedItem item;
    private String webviewData;

    private ItemActionButton actionButton1;
    private ItemActionButton actionButton2;
    private Disposable disposable;
    private Disposable contentCrunchDisposable;
    private FeeditemFragmentBinding viewBinding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        itemId = getArguments().getLong(ARG_FEEDITEM);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        viewBinding = FeeditemFragmentBinding.inflate(inflater, container, false);
        viewBinding.header.setVisibility(View.INVISIBLE);
        viewBinding.txtvPodcast.setOnClickListener(v -> openPodcast());
        viewBinding.txtvTitle.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_FULL);
        viewBinding.txtvTitle.setEllipsize(TextUtils.TruncateAt.END);
        viewBinding.webvDescription.setTimecodeSelectedListener(time -> {
            if (!PlaybackService.isRunning) {
                EventBus.getDefault().post(
                        new MessageEvent(getString(R.string.play_this_to_seek_position_message)));
                return;
            }
            if (BuildConfig.USE_MEDIA3_PLAYBACK_SERVICE) {
                PlaybackController.bindToMedia3Service(getActivity(), controller ->
                        controller.seekTo(time));
                return;
            }
            PlaybackController.bindToService(getActivity(), playbackService -> {
                if (item.getMedia() != null && playbackService.getPlayable() != null
                        && Objects.equals(item.getMedia().getIdentifier(),
                        playbackService.getPlayable().getIdentifier())) {
                    playbackService.seekTo(time);
                } else {
                    EventBus.getDefault().post(
                            new MessageEvent(getString(R.string.play_this_to_seek_position_message)));
                }
            });
        });
        registerForContextMenu(viewBinding.webvDescription);
        viewBinding.imgvCover.setOnClickListener(v -> openPodcast());
        viewBinding.butAction1.setOnClickListener(v -> {
            if (actionButton1 instanceof StreamActionButton && !UserPreferences.isStreamOverDownload()
                    && UsageStatistics.hasSignificantBiasTo(UsageStatistics.ACTION_STREAM)) {
                showOnDemandConfigBalloon(true);
                return;
            } else if (actionButton1 == null) {
                return; // Not loaded yet
            }
            actionButton1.onClick(getContext());
        });
        viewBinding.butAction2.setOnClickListener(v -> {
            if (actionButton2 instanceof DownloadActionButton && UserPreferences.isStreamOverDownload()
                    && UsageStatistics.hasSignificantBiasTo(UsageStatistics.ACTION_DOWNLOAD)) {
                showOnDemandConfigBalloon(false);
                return;
            } else if (actionButton2 == null) {
                return; // Not loaded yet
            }
            actionButton2.onClick(getContext());
        });
        viewBinding.contentCrunchButton.setOnClickListener(v -> requestContentCrunchSummary());
        viewBinding.txtvPodcast.setOnLongClickListener(v -> {
            ClipboardUtils.copyText(viewBinding.txtvPodcast);
            return true;
        });
        viewBinding.txtvTitle.setOnLongClickListener(v -> {
            ClipboardUtils.copyText(viewBinding.txtvTitle);
            return true;
        });
        return viewBinding.getRoot();
    }

    private void showOnDemandConfigBalloon(boolean offerStreaming) {
        final boolean isLocaleRtl = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())
                == View.LAYOUT_DIRECTION_RTL;
        final Balloon balloon = new Balloon.Builder(getContext())
                .setArrowOrientation(ArrowOrientation.TOP)
                .setArrowOrientationRules(ArrowOrientationRules.ALIGN_FIXED)
                .setArrowPosition(0.25f + ((isLocaleRtl ^ offerStreaming) ? 0f : 0.5f))
                .setWidthRatio(1.0f)
                .setMarginLeft(8)
                .setMarginRight(8)
                .setBackgroundColor(ThemeUtils.getColorFromAttr(getContext(), R.attr.colorSecondary))
                .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
                .setLayout(R.layout.popup_bubble_view)
                .setDismissWhenTouchOutside(true)
                .setLifecycleOwner(this)
                .build();
        final Button positiveButton = balloon.getContentView().findViewById(R.id.balloon_button_positive);
        final Button negativeButton = balloon.getContentView().findViewById(R.id.balloon_button_negative);
        final TextView message = balloon.getContentView().findViewById(R.id.balloon_message);
        message.setText(offerStreaming
                ? R.string.on_demand_config_stream_text : R.string.on_demand_config_download_text);
        positiveButton.setOnClickListener(v1 -> {
            UserPreferences.setStreamOverDownload(offerStreaming);
            // Update all visible lists to reflect new streaming action button
            EventBus.getDefault().post(new FeedItemEvent(Collections.emptyList(), true));
            EventBus.getDefault().post(new MessageEvent(getString(R.string.on_demand_config_setting_changed)));
            balloon.dismiss();
        });
        negativeButton.setOnClickListener(v1 -> {
            UsageStatistics.doNotAskAgain(UsageStatistics.ACTION_STREAM); // Type does not matter. Both are silenced.
            balloon.dismiss();
        });
        balloon.showAlignBottom(viewBinding.butAction1, 0, (int) (-12 * getResources().getDisplayMetrics().density));
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        load();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (itemsLoaded) {
            viewBinding.progbarLoading.setVisibility(View.GONE);
            updateAppearance();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (disposable != null) {
            disposable.dispose();
        }
        if (contentCrunchDisposable != null) {
            contentCrunchDisposable.dispose();
            contentCrunchDisposable = null;
        }
        viewBinding.contentRoot.removeView(viewBinding.webvDescription);
        viewBinding.webvDescription.destroy();
        viewBinding = null;
    }

    private void onFragmentLoaded() {
        if (webviewData != null && !itemsLoaded) {
            viewBinding.webvDescription.loadDataWithBaseURL(
                    "https://127.0.0.1", webviewData, "text/html", "utf-8", "about:blank");
        }
        updateAppearance();
    }

    private void updateAppearance() {
        if (item == null) {
            Log.d(TAG, "updateAppearance item is null");
            return;
        }
        viewBinding.txtvPodcast.setText(item.getFeed().getTitle());
        viewBinding.txtvTitle.setText(item.getTitle());
        if (item.getPubDate() != null) {
            String pubDateStr = DateFormatter.formatAbbrev(getActivity(), item.getPubDate());
            viewBinding.txtvPublished.setText(pubDateStr);
            viewBinding.txtvPublished.setContentDescription(DateFormatter.formatForAccessibility(item.getPubDate()));
        }
        if (item.getFeed().getState() == Feed.STATE_NOT_SUBSCRIBED) {
            viewBinding.nonSubscribedWarningLabel.setVisibility(View.VISIBLE);
            viewBinding.nonSubscribedWarningLabel.setOnClickListener(v -> openPodcast());
        }
        float radius = 8 * getResources().getDisplayMetrics().density;
        RequestOptions options = new RequestOptions()
                .error(ImagePlaceholder.getDrawable(getContext(), radius))
                .transform(new FitCenter(),
                        new RoundedCorners((int) radius))
                .dontAnimate();
        Glide.with(this)
                .load(item.getImageLocation())
                .error(Glide.with(this)
                        .load(ImageResourceUtils.getFallbackImageLocation(item))
                        .apply(options))
                .apply(options)
                .into(viewBinding.imgvCover);
        updateButtons();
    }

    private void updateButtons() {
        viewBinding.circularProgressBar.setVisibility(View.GONE);
        if (item.hasMedia()) {
            if (DownloadServiceInterface.get().isDownloadingEpisode(item.getMedia().getDownloadUrl())) {
                viewBinding.circularProgressBar.setVisibility(View.VISIBLE);
                viewBinding.circularProgressBar.setPercentage(0.01f * Math.max(1,
                        DownloadServiceInterface.get().getProgress(item.getMedia().getDownloadUrl())), item);
                viewBinding.circularProgressBar.setIndeterminate(
                        DownloadServiceInterface.get().isEpisodeQueued(item.getMedia().getDownloadUrl()));
            }
        }
        FeedMedia media = item.getMedia();
        if (media == null) {
            actionButton1 = new MarkAsPlayedActionButton(item);
            actionButton2 = new VisitWebsiteActionButton(item);
            viewBinding.noMediaLabel.setVisibility(View.VISIBLE);
            viewBinding.txtvDuration.setVisibility(View.GONE);
            viewBinding.separatorIcons.setVisibility(View.GONE);
        } else {
            viewBinding.noMediaLabel.setVisibility(View.GONE);
            boolean hasDuration = media.getDuration() > 0;
            viewBinding.txtvDuration.setVisibility(hasDuration ? View.VISIBLE : View.GONE);
            viewBinding.separatorIcons.setVisibility(hasDuration ? View.VISIBLE : View.GONE);
            if (hasDuration) {
                viewBinding.txtvDuration.setText(Converter.getDurationStringLong(media.getDuration()));
                viewBinding.txtvDuration.setContentDescription(
                        Converter.getDurationStringLocalized(getContext(), media.getDuration()));
            }
            if (PlaybackStatus.isCurrentlyPlaying(media)) {
                actionButton1 = new PauseActionButton(item);
            } else if (item.getFeed().isLocalFeed()) {
                actionButton1 = new PlayLocalActionButton(item);
            } else if (media.isDownloaded()) {
                actionButton1 = new PlayActionButton(item);
            } else {
                actionButton1 = new StreamActionButton(item);
            }
            if (DownloadServiceInterface.get().isDownloadingEpisode(media.getDownloadUrl())) {
                actionButton2 = new CancelDownloadActionButton(item);
            } else if (item.getFeed().isLocalFeed() || media.isDownloaded()) {
                actionButton2 = new DeleteActionButton(item);
            } else {
                actionButton2 = new DownloadActionButton(item);
            }
        }

        viewBinding.butAction1Text.setText(actionButton1.getLabel());
        viewBinding.butAction1Text.setTransformationMethod(null);
        viewBinding.butAction1Icon.setImageResource(actionButton1.getDrawable());
        viewBinding.butAction1.setVisibility(actionButton1.getVisibility());

        viewBinding.butAction2Text.setText(actionButton2.getLabel());
        viewBinding.butAction2Text.setTransformationMethod(null);
        viewBinding.butAction2Icon.setImageResource(actionButton2.getDrawable());
        viewBinding.butAction2.setVisibility(actionButton2.getVisibility());
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return viewBinding.webvDescription.onContextItemSelected(item);
    }

    private void openPodcast() {
        if (item == null) {
            return;
        }
        if (item.getFeed().getState() == Feed.STATE_NOT_SUBSCRIBED) {
            startActivity(new OnlineFeedviewActivityStarter(getContext(), item.getFeed().getDownloadUrl())
                    .getIntent());
        } else {
            Fragment fragment = FeedItemlistFragment.newInstance(item.getFeedId());
            ((MainActivity) getActivity()).loadChildFragment(fragment);
        }
    }

    private void requestContentCrunchSummary() {
        if (item == null || item.getMedia() == null) {
            return;
        }
        ContentCrunchModels.EpisodeKey key = EpisodeMatcher.from(item);
        if (!EpisodeMatcher.isValid(key)) {
            showContentCrunchState(getString(R.string.content_crunch_invalid_episode), false);
            return;
        }
        ContentCrunchModels.EpisodeResult cached = ContentCrunchCache.get(key);
        if (cached != null && !cached.summary.isEmpty()) {
            showContentCrunchState(cached.summary, false);
            return;
        }
        showSummaryConfiguration(key);
    }

    private void showSummaryConfiguration(ContentCrunchModels.EpisodeKey key) {
        ContentCrunchPreferences preferences = new ContentCrunchPreferences(requireContext());
        SummaryConfig current = preferences.getSummaryConfig();
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, 0, padding, 0);
        Spinner sizes = new Spinner(requireContext());
        String[] sizeLabels = getResources().getStringArray(R.array.content_crunch_summary_sizes);
        sizes.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, sizeLabels));
        int selectedSize = 0;
        for (int i = 0; i < SummaryConfig.SIZES.length; i++) {
            if (SummaryConfig.SIZES[i] == current.sizeWords) { selectedSize = i; }
        }
        sizes.setSelection(selectedSize);
        Spinner styles = new Spinner(requireContext());
        String[] styleLabels = getResources().getStringArray(R.array.content_crunch_summary_styles);
        styles.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, styleLabels));
        int selectedStyle = java.util.Arrays.asList(SummaryConfig.STYLES).indexOf(current.style);
        styles.setSelection(Math.max(0, selectedStyle));
        content.addView(sizes); content.addView(styles);
        new AlertDialog.Builder(requireContext()).setTitle(R.string.content_crunch_configure_summary)
                .setView(content).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.content_crunch_process, (dialog, which) -> {
                    SummaryConfig config = new SummaryConfig(SummaryConfig.SIZES[sizes.getSelectedItemPosition()],
                            SummaryConfig.STYLES[styles.getSelectedItemPosition()]);
                    preferences.setSummaryConfig(config);
                    processSummary(key, config);
                }).show();
    }

    private void processSummary(ContentCrunchModels.EpisodeKey key, SummaryConfig config) {
        viewBinding.contentCrunchButton.setEnabled(false);
        showContentCrunchState(getString(R.string.content_crunch_waiting_transcript), true);
        final StringBuilder partial = new StringBuilder();
        contentCrunchDisposable = Maybe.fromCallable(() -> ContentCrunchClient.get(requireContext())
                .processAndObserve(key, config, event -> {
                    if (!isAdded() || viewBinding == null) { return false; }
                    final JSONObject data;
                    try { data = new JSONObject(event.data); }
                    catch (JSONException error) { throw new IOException(error); }
                    if (event.type.equals("summary.delta")) {
                        String updated = ContentCrunchSseParser.appendDelta(partial.toString(),
                                data.optInt("offset", partial.length()), data.optString("text"));
                        partial.setLength(0); partial.append(updated);
                        android.app.Activity activity = getActivity();
                        if (activity != null) {
                            activity.runOnUiThread(() -> showContentCrunchState(updated, true));
                        }
                    } else if (event.type.equals("snapshot")) {
                        String snapshot = data.optString("partialSummary", "");
                        if (!snapshot.isEmpty()) {
                            partial.setLength(0); partial.append(snapshot);
                            android.app.Activity activity = getActivity();
                            if (activity != null) {
                                activity.runOnUiThread(() -> showContentCrunchState(snapshot, true));
                            }
                        }
                    } else if (event.type.equals("stage")) {
                        String message = data.optString("message", data.optString("stage", ""));
                        if (!message.isEmpty()) {
                            android.app.Activity activity = getActivity();
                            if (activity != null) {
                                activity.runOnUiThread(() -> showContentCrunchState(message, true));
                            }
                        }
                    }
                    return !event.type.equals("completed") && !event.type.equals("failed");
                }))
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    ContentCrunchCache.put(key, result);
                    viewBinding.contentCrunchButton.setEnabled(true);
                    if (ContentCrunchPoller.isFailed(result)) {
                        showContentCrunchState(getString(R.string.content_crunch_failed), false);
                    } else if (ContentCrunchPoller.isCompleted(result)) {
                        showContentCrunchState(result.summary, false);
                    } else {
                        showContentCrunchState(getString(R.string.content_crunch_still_processing), true);
                    }
                }, error -> {
                    viewBinding.contentCrunchButton.setEnabled(true);
                    showContentCrunchState(getString(R.string.content_crunch_unavailable), false);
                });
    }

    private void showContentCrunchState(String message, boolean loading) {
        if (viewBinding == null) { return; }
        viewBinding.contentCrunchSummaryBlock.setVisibility(View.VISIBLE);
        viewBinding.contentCrunchProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        viewBinding.contentCrunchSummaryText.setText(ContentCrunchSummaryFormatter.format(message));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        if (event.unreadStatusChanged && event.items.isEmpty()) {
            load();
            return;
        }
        for (FeedItem item : event.items) {
            if (this.item != null && this.item.getId() == item.getId()) {
                load();
                return;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedListUpdateEvent event) {
        if (item != null && item.getFeed() != null && event.contains(item.getFeed())) {
            load();
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EpisodeDownloadEvent event) {
        if (item == null || item.getMedia() == null) {
            return;
        }
        if (!event.getUrls().contains(item.getMedia().getDownloadUrl())) {
            return;
        }
        if (itemsLoaded && getActivity() != null) {
            updateButtons();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusChanged(PlayerStatusEvent event) {
        updateButtons();
    }

    private void load() {
        if (disposable != null) {
            disposable.dispose();
        }
        if (!itemsLoaded) {
            viewBinding.progbarLoading.setVisibility(View.VISIBLE);
        }
        disposable = Maybe.fromCallable(this::loadInBackground)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(result -> {
                viewBinding.progbarLoading.setVisibility(View.GONE);
                viewBinding.header.setVisibility(View.VISIBLE);
                item = result;
                onFragmentLoaded();
                itemsLoaded = true;
            }, error -> Log.e(TAG, Log.getStackTraceString(error)),
                    () -> requireActivity().getSupportFragmentManager().popBackStack());
    }

    @Nullable
    private FeedItem loadInBackground() {
        FeedItem feedItem = DBReader.getFeedItem(itemId);
        Context context = getContext();
        if (feedItem != null && context != null) {
            int duration = feedItem.getMedia() != null ? feedItem.getMedia().getDuration() : Integer.MAX_VALUE;
            DBReader.loadDescriptionOfFeedItem(feedItem);
            ShownotesCleaner t = new ShownotesCleaner(context, feedItem.getDescription(), duration);
            webviewData = t.processShownotes();
        }
        return feedItem;
    }

}
