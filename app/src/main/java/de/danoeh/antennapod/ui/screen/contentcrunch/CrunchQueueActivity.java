package de.danoeh.antennapod.ui.screen.contentcrunch;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.net.contentcrunch.ContentCrunchCache;
import de.danoeh.antennapod.net.contentcrunch.ContentCrunchClient;
import de.danoeh.antennapod.net.contentcrunch.ContentCrunchModels;
import de.danoeh.antennapod.net.contentcrunch.ContentCrunchPoller;
import de.danoeh.antennapod.net.contentcrunch.CrunchQueuePolicy;
import de.danoeh.antennapod.net.contentcrunch.CrunchQueueStore;
import de.danoeh.antennapod.net.contentcrunch.EpisodeMatcher;
import de.danoeh.antennapod.net.contentcrunch.ContentCrunchPreferences;
import de.danoeh.antennapod.net.contentcrunch.SummaryConfig;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.ui.common.ToolbarActivity;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Summary-first inbox for recent new and unplayed episodes. */
public class CrunchQueueActivity extends ToolbarActivity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final List<FeedItem> items = new ArrayList<>();
    private final Map<Long, ContentCrunchModels.EpisodeResult> results = new HashMap<>();
    private final Set<Long> processing = new java.util.HashSet<>();
    private QueueAdapter adapter;
    private CrunchQueueStore store;
    private boolean showDismissed;
    private boolean loaded;
    private TextView empty;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.crunch_queue_activity);
        setTitle(R.string.content_crunch_queue_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        store = new CrunchQueueStore(this);
        empty = findViewById(R.id.emptyView);
        empty.setText(R.string.content_crunch_queue_loading);
        empty.setVisibility(View.VISIBLE);
        RecyclerView list = findViewById(R.id.recyclerView);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QueueAdapter();
        list.setAdapter(adapter);
        load();
    }

    private void load() {
        worker.execute(() -> {
            List<FeedItem> loaded = DBReader.getEpisodes(0, 100,
                    new FeedItemFilter(FeedItemFilter.UNPLAYED, FeedItemFilter.HAS_MEDIA), SortOrder.DATE_NEW_OLD);
            Set<Long> dismissed = store.getDismissedIds();
            loaded.removeIf(item -> !showDismissed && dismissed.contains(item.getId()));
            runOnUiThread(() -> {
                if (isDestroyed()) { return; }
                items.clear(); items.addAll(loaded);
                CrunchQueueActivity.this.loaded = true;
                adapter.notifyDataSetChanged();
                empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(R.string.content_crunch_show_dismissed).setCheckable(true).setChecked(showDismissed)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(R.string.content_crunch_reset_dismissed).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        if (item.getTitle().equals(getString(R.string.content_crunch_show_dismissed))) {
            showDismissed = !showDismissed; item.setChecked(showDismissed); load(); return true;
        }
        if (item.getTitle().equals(getString(R.string.content_crunch_reset_dismissed))) {
            store.reset(); Toast.makeText(this, R.string.content_crunch_dismissed_reset, Toast.LENGTH_SHORT).show();
            load(); return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void process(FeedItem item) {
        ContentCrunchPreferences preferences = new ContentCrunchPreferences(this);
        SummaryConfig current = preferences.getSummaryConfig();
        String[] styles = getResources().getStringArray(R.array.content_crunch_summary_styles);
        int selected = Math.max(0, java.util.Arrays.asList(SummaryConfig.STYLES).indexOf(current.style));
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(R.string.content_crunch_configure_summary)
                .setSingleChoiceItems(styles, selected, null).setPositiveButton(R.string.content_crunch_next,
                        (dialog, which) -> {
                            int style = ((androidx.appcompat.app.AlertDialog) dialog).getListView()
                                    .getCheckedItemPosition();
                            chooseSizeAndProcess(item, preferences, SummaryConfig.STYLES[Math.max(0, style)]);
                        }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void chooseSizeAndProcess(FeedItem item, ContentCrunchPreferences preferences, String style) {
        SummaryConfig current = preferences.getSummaryConfig();
        String[] sizes = getResources().getStringArray(R.array.content_crunch_summary_sizes);
        int selected = 0;
        for (int i = 0; i < SummaryConfig.SIZES.length; i++) { if (SummaryConfig.SIZES[i] == current.sizeWords) { selected = i; } }
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(R.string.content_crunch_summary_size)
                .setSingleChoiceItems(sizes, selected, null).setPositiveButton(R.string.content_crunch_process,
                        (dialog, which) -> {
                            int size = ((androidx.appcompat.app.AlertDialog) dialog).getListView()
                                    .getCheckedItemPosition();
                            SummaryConfig config = new SummaryConfig(SummaryConfig.SIZES[Math.max(0, size)], style);
                            preferences.setSummaryConfig(config);
                            process(item, config);
                        }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void process(FeedItem item, SummaryConfig config) {
        if (processing.contains(item.getId())) { return; }
        ContentCrunchModels.EpisodeKey key = EpisodeMatcher.from(item);
        if (!EpisodeMatcher.isValid(key)) {
            Toast.makeText(this, R.string.content_crunch_invalid_episode, Toast.LENGTH_SHORT).show(); return;
        }
        processing.add(item.getId()); adapter.notifyDataSetChanged();
        worker.execute(() -> {
            ContentCrunchModels.EpisodeResult result;
            try { result = ContentCrunchClient.get(this).processAndPoll(key, config); }
            catch (Exception error) { result = new ContentCrunchModels.EpisodeResult("failed", null, null); }
            ContentCrunchModels.EpisodeResult finalResult = result;
            if (ContentCrunchPoller.isCompleted(result)) { ContentCrunchCache.put(key, result); }
            runOnUiThread(() -> {
                if (isDestroyed()) { return; }
                processing.remove(item.getId()); results.put(item.getId(), finalResult); adapter.notifyDataSetChanged();
            });
        });
    }

    @Override protected void onDestroy() { worker.shutdownNow(); super.onDestroy(); }

    private final class QueueAdapter extends RecyclerView.Adapter<Row> {
        @NonNull @Override public Row onCreateViewHolder(@NonNull android.view.ViewGroup parent, int type) {
            return new Row(getLayoutInflater().inflate(R.layout.crunch_queue_item, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull Row row, int position) { row.bind(items.get(position)); }
        @Override public int getItemCount() { return items.size(); }
    }

    private final class Row extends RecyclerView.ViewHolder {
        final TextView title, podcast, state, summary, provenance;
        final Button process, queue;
        final ImageButton overflow;
        Row(View view) {
            super(view); title = view.findViewById(R.id.title); podcast = view.findViewById(R.id.podcast);
            state = view.findViewById(R.id.state); summary = view.findViewById(R.id.summary);
            provenance = view.findViewById(R.id.provenance);
            process = view.findViewById(R.id.process); queue = view.findViewById(R.id.queue);
            overflow = view.findViewById(R.id.overflow);
        }
        void bind(FeedItem item) {
            title.setText(item.getTitle());
            podcast.setText(item.getFeed() == null ? "" : item.getFeed().getTitle());
            ContentCrunchModels.EpisodeKey key = EpisodeMatcher.from(item);
            ContentCrunchModels.EpisodeResult result = results.containsKey(item.getId())
                    ? results.get(item.getId()) : ContentCrunchCache.get(key);
            String current = CrunchQueuePolicy.state(result, processing.contains(item.getId()));
            int stateText = current.equals("ready") ? R.string.content_crunch_ready
                    : current.equals("failed") ? R.string.content_crunch_failed
                    : current.equals("waiting_transcript") ? R.string.content_crunch_waiting_transcript
                    : current.equals("generating_skip_analysis") ? R.string.content_crunch_generating_skip
                    : current.equals("generating_summary") ? R.string.content_crunch_generating_summary
                    : R.string.content_crunch_not_processed;
            state.setText(stateText);
            String excerpt = CrunchQueuePolicy.excerpt(result == null ? null : result.summary, 240);
            summary.setText(excerpt.isEmpty() ? getString(R.string.content_crunch_no_summary) : excerpt);
            provenance.setVisibility(excerpt.isEmpty() ? View.GONE : View.VISIBLE);
            summary.setOnClickListener(v -> summary.setMaxLines(summary.getMaxLines() == 6 ? Integer.MAX_VALUE : 6));
            title.setOnClickListener(v -> new MainActivityStarter(CrunchQueueActivity.this)
                    .withOpenEpisode(item.getId()).start());
            process.setEnabled(!processing.contains(item.getId()));
            process.setText(current.equals("failed") ? R.string.content_crunch_retry : R.string.content_crunch_process);
            process.setVisibility(current.equals("ready") ? View.GONE : View.VISIBLE);
            process.setOnClickListener(v -> process(item));
            queue.setEnabled(!item.isTagged(FeedItem.TAG_QUEUE));
            queue.setOnClickListener(v -> { DBWriter.addQueueItem(CrunchQueueActivity.this, item); queue.setEnabled(false); });
            overflow.setOnClickListener(v -> showOverflow(item));
        }
        private void showOverflow(FeedItem item) {
            PopupMenu menu = new PopupMenu(CrunchQueueActivity.this, overflow);
            menu.getMenu().add(R.string.content_crunch_archive);
            menu.getMenu().add(R.string.content_crunch_dismiss);
            menu.setOnMenuItemClickListener(action -> {
                boolean archive = action.getTitle().equals(getString(R.string.content_crunch_archive));
                if (archive) { DBWriter.markItemsPlayed(FeedItem.PLAYED, false, java.util.Collections.singletonList(item)); }
                else { store.dismiss(item.getId()); }
                remove(item);
                Snackbar.make(findViewById(android.R.id.content), archive ? R.string.content_crunch_archived
                        : R.string.content_crunch_dismissed, Snackbar.LENGTH_LONG).setAction(R.string.undo, v -> {
                            if (archive) { DBWriter.markItemsPlayed(FeedItem.UNPLAYED, false,
                                    java.util.Collections.singletonList(item)); } else { store.restore(item.getId()); }
                            load();
                        }).show();
                return true;
            });
            menu.show();
        }
        private void remove(FeedItem item) {
            int index = items.indexOf(item); if (index >= 0) { items.remove(index); adapter.notifyItemRemoved(index); }
            empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
}
