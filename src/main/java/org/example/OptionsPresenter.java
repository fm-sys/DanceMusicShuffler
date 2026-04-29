package org.example;

import org.example.worker.PersistentPreferences;
import org.example.worker.PlaylistLoader;
import org.example.worker.ShuffleAlgorithm;
import org.example.util.Scheduler;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;


public class OptionsPresenter {

    private OptionsView view;

    private final PlaylistStore playlistStore;
    private final PreferencesStore preferencesStore;
    private final ShuffleAlgorithm shuffleAlgorithm;
    private final FilterStore filterStore;

    public OptionsPresenter(PlaylistStore playlistStore, PreferencesStore preferencesStore, FilterStore filterStore, ShuffleAlgorithm shuffleAlgorithm) {
        this.playlistStore = playlistStore;
        this.preferencesStore = preferencesStore;
        this.shuffleAlgorithm = shuffleAlgorithm;
        this.filterStore = filterStore;
    }

    public void init(OptionsView view) {
        this.view = view;

        preferencesStore.subscribe(this::updatePreferences);
    }

    public void updatePreferences(PreferenceParams params) {
        SwingUtilities.invokeLater(() -> view.applyPreferences(params));
    }

    public void onLoadPrefsClicked() {
        PersistentPreferences.loadAsync(playlistStore, filterStore, preferencesStore, "prefs.json");
    }

    public void onStorePrefsClicked() {
        PersistentPreferences.store(playlistStore, filterStore, preferencesStore.get());
    }

    public void onCountChanged(int count) {
        preferencesStore.updateCount(count);
    }

    public void onCooldownChanged(int cooldown) {
        preferencesStore.updateCooldown(cooldown);
    }

    public void onGroupPlaylistsChanged(boolean groupPlaylists) {
        preferencesStore.updateGroupPlaylists(groupPlaylists);
    }

    public void onSecondaryShowSideSheetChanged(boolean show) {
        preferencesStore.updateShowSidePanel(show);
    }

    public void onSecondaryCoverChanged(boolean show) {
        preferencesStore.updateShowCover(show);
    }

    public void onSecondaryColoredBackgroundChanged(boolean colored) {
        preferencesStore.updateShowBackground(colored);
    }

    public void onExclusiveClicked() {
        SwingUtilities.invokeLater(() -> view.showExclusivePoolDialog(playlistStore.getSelectedPlaylists()));
    }

    public void onWeightsClicked() {
        SwingUtilities.invokeLater(() -> view.showWeightsDialog(playlistStore.getSelectedPlaylists()));
    }

    public void onLoadAndShuffleClicked() {
        CompletableFuture<Boolean> loadFuture = PlaylistLoader.loadPlaylistsAsync(playlistStore.getSelectedPlaylists());
        loadFuture.thenAccept(success -> {
            if (success) {
                shuffleAlgorithm.shuffleAsync(preferencesStore.get()).thenAccept(result -> Scheduler.waitForWebApiDelayAndRun(() -> shuffleFinished(result)));
            } else {
                shuffleFinished(false);
            }
        }).whenComplete((r, ex) -> {
            if (ex != null) {
                System.err.println("Caught Exception: " + ex.getMessage());
                shuffleFinished(false);
            }
        });
    }

    private void shuffleFinished(boolean success) {
        SwingUtilities.invokeLater(() -> view.loadAndShuffleFinished(success));
    }
}


