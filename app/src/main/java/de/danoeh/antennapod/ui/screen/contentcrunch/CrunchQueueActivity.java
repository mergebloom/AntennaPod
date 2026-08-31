package de.danoeh.antennapod.ui.screen.contentcrunch;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.ui.common.ToolbarActivity;
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
    private TextView empty;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.crunch_queue_activity);
        setTitle(R.string.content_crunch_queue_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        store = new CrunchQueueStore(this);
        empty = findViewById(R.id.emptyView);
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
        if (processing.contains(item.getId())) { return; }
        ContentCrunchModels.EpisodeKey key = EpisodeMatcher.from(item);
        if (!EpisodeMatcher.isValid(key)) {
            Toast.makeText(this, R.string.content_crunch_invalid_episode, Toast.LENGTH_SHORT).show(); return;
        }
        processing.add(item.getId()); adapter.notifyDataSetChanged();
        worker.execute(() -> {
            ContentCrunchModels.EpisodeResult result;
            try { result = ContentCrunchClient.get(this).processAndPoll(key); }
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
        final TextView title, podcast, state, summary;
        final Button process, queue, archive, dismiss;
        Row(View view) {
            super(view); title = view.findViewById(R.id.title); podcast = view.findViewById(R.id.podcast);
            state = view.findViewById(R.id.state); summary = view.findViewById(R.id.summary);
            process = view.findViewById(R.id.process); queue = view.findViewById(R.id.queue);
            archive = view.findViewById(R.id.archive); dismiss = view.findViewById(R.id.dismiss);
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
                    : current.equals("processing") ? R.string.content_crunch_processing
                    : R.string.content_crunch_not_processed;
            state.setText(stateText);
            String excerpt = CrunchQueuePolicy.excerpt(result == null ? null : result.summary, 240);
            summary.setText(excerpt.isEmpty() ? getString(R.string.content_crunch_no_summary) : excerpt);
            process.setEnabled(!processing.contains(item.getId()));
            process.setText(current.equals("failed") ? R.string.content_crunch_retry : R.string.content_crunch_process);
            process.setVisibility(current.equals("ready") ? View.GONE : View.VISIBLE);
            process.setOnClickListener(v -> process(item));
            queue.setEnabled(!item.isTagged(FeedItem.TAG_QUEUE));
            queue.setOnClickListener(v -> { DBWriter.addQueueItem(CrunchQueueActivity.this, item); queue.setEnabled(false); });
            archive.setOnClickListener(v -> { DBWriter.markItemsPlayed(FeedItem.PLAYED, false,
                    java.util.Collections.singletonList(item)); remove(item); });
            dismiss.setOnClickListener(v -> { store.dismiss(item.getId()); remove(item); });
        }
        private void remove(FeedItem item) {
            int index = items.indexOf(item); if (index >= 0) { items.remove(index); adapter.notifyItemRemoved(index); }
            empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
}
