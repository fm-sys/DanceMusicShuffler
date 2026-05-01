package fmsys.musicshuffler.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import fmsys.musicshuffler.store.FilterStore;
import fmsys.musicshuffler.store.PlaylistStore;
import fmsys.musicshuffler.store.PreferenceParams;
import fmsys.musicshuffler.store.PreferencesStore;
import fmsys.musicshuffler.model.PlaylistModel;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PersistentPreferences {

    int count = 0;
    int cooldown = 0;
    boolean groupPlaylists = false;
    String searchString = null;
    boolean showSidePanel = true;
    boolean showCover = true;
    boolean showBackground = true;
    List<PersistentPlaylistModel> playlists = null;

    public PersistentPreferences() {
        // Default constructor for Jackson
    }

    public PersistentPreferences(PlaylistStore playlistStore, FilterStore filterStore, PreferenceParams params) {
        this.playlists = playlistStore.get().stream().filter(PlaylistModel::hasModifiedConfig).map(pl -> new PersistentPlaylistModel(pl.getPlaylist().getId(), pl.isChecked(), pl.isExclusive(), pl.getWeight())).toList();
        this.count = params.count();
        this.cooldown = params.cooldown();
        this.groupPlaylists = params.groupPlaylists();
        this.searchString = filterStore.get();
        this.showSidePanel = params.showSidePanel();
        this.showCover = params.showCover();
        this.showBackground = params.showBackground();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class PersistentPlaylistModel {
        String id;
        boolean checked;
        boolean exclusive;
        double weight;

        public PersistentPlaylistModel() {
            // Default constructor for Jackson
        }

        public PersistentPlaylistModel(String id, boolean checked, boolean exclusive, double weight) {
            this.id = id;
            this.checked = checked;
            this.exclusive = exclusive;
            this.weight = weight;
        }
    }

    private PreferenceParams toPreferenceParams() {
        return new PreferenceParams(count, cooldown, groupPlaylists, showSidePanel, showCover, showBackground);
    }

    /**
     * Load the persistent preferences from a file.
     * The file is expected to be in JSON format.
     *
     * @param playlistStore The playlist store object to be enriched.
     */
    public static CompletableFuture<Boolean> loadAsync(PlaylistStore playlistStore, FilterStore filterStore, PreferencesStore preferencesStore, String fileName) {
        return CompletableFuture.supplyAsync(() -> load(playlistStore, filterStore, preferencesStore, fileName));
    }

    private static boolean load(PlaylistStore playlistStore, FilterStore filterStore, PreferencesStore preferencesStore, String fileName) {
        ObjectMapper objectMapper = new ObjectMapper();
        boolean couldLoad = false;

        try {
            // Read JSON from a file and convert it to a Java object
            PersistentPreferences preferences = objectMapper.readValue(new File(fileName), PersistentPreferences.class);
            couldLoad = true;

            filterStore.setState(preferences.searchString);

            // Enrich the playlists with the loaded preferences
            for (PersistentPlaylistModel persistentPlaylist : preferences.playlists) {
                boolean found = false;
                for (PlaylistModel playlist : playlistStore.get()) {
                    if (playlist.getPlaylist().getId().equals(persistentPlaylist.id)) {
                        playlist.setChecked(persistentPlaylist.checked);
                        playlist.setExclusive(persistentPlaylist.exclusive);
                        playlist.setWeight(persistentPlaylist.weight);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    PlaylistLoader.loadPlaylistModelFromPrefs(persistentPlaylist, playlistStore);
                }
            }
            preferencesStore.setState(preferences.toPreferenceParams());
        } catch (IOException e) {
            System.out.println("Failed to load preferences: " + e.getMessage());
        }
        return couldLoad;
    }

    public static void store(PlaylistStore playlistStore, FilterStore filterStore, PreferenceParams params) {
        PersistentPreferences preferences = new PersistentPreferences(playlistStore, filterStore, params);
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // Write object as JSON to a file
            objectMapper.writeValue(new File("prefs.json"), preferences);

            System.out.println("JSON written to file successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
