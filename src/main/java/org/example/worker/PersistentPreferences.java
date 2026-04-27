package org.example.worker;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.PlaylistStore;
import org.example.models.PlaylistModel;
import se.michaelthelin.spotify.SpotifyApiThreading;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PersistentPreferences {

    int count = 0;
    int cooldown = 0;
    boolean groupPlaylists = false;
    String searchString = null;
    boolean showSidePanel = true;
    boolean showCover = true;
    boolean colorBackground = true;
    List<PersistentPlaylistModel> playlists = null;

    public PersistentPreferences() {
        // Default constructor for Jackson
    }

    public PersistentPreferences(PlaylistStore playlistStore, MainGuiParams params) {
        this.playlists = playlistStore.get().stream().filter(PlaylistModel::hasModifiedConfig).map(pl -> new PersistentPlaylistModel(pl.getPlaylist().getId(), pl.isChecked(), pl.isExclusive(), pl.getWeight())).toList();
        this.count = params.count;
        this.cooldown = params.cooldown;
        this.groupPlaylists = params.groupPlaylists;
        this.searchString = params.searchString;
        this.showSidePanel = params.showSidePanel;
        this.showCover = params.showCover;
        this.colorBackground = params.colorBackground;
    }

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

    public static class MainGuiParams {
        public int count;
        public int cooldown;
        public boolean groupPlaylists;
        public String searchString;
        public boolean showSidePanel;
        public boolean showCover;
        public boolean colorBackground;

        private MainGuiParams(int count, int cooldown, boolean groupPlaylists, String searchString, boolean showSidePanel, boolean showCover, boolean colorBackground) {
            this.count = count;
            this.cooldown = cooldown;
            this.groupPlaylists = groupPlaylists;
            this.searchString = searchString;
            this.showSidePanel = showSidePanel;
            this.showCover = showCover;
            this.colorBackground = colorBackground;
        }
    }

    /**
     * Load the persistent preferences from a file.
     * The file is expected to be in JSON format.
     *
     * @param playlistStore The playlist store object to be enriched.
     */
    public static CompletableFuture<MainGuiParams> loadAsync(PlaylistStore playlistStore, String fileName) {
        return SpotifyApiThreading.executeAsync(() -> load(playlistStore, fileName));
    }

    private static MainGuiParams load(PlaylistStore playlistStore, String fileName) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // Read JSON from a file and convert it to a Java object
            PersistentPreferences preferences = objectMapper.readValue(new File(fileName), PersistentPreferences.class);

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
            return new MainGuiParams(preferences.count, preferences.cooldown, preferences.groupPlaylists, preferences.searchString, preferences.showSidePanel, preferences.showCover, preferences.colorBackground);
        } catch (IOException e) {
            System.out.println("Failed to load preferences: " + e.getMessage());
            return null;
        }
    }

    public static void store(PlaylistStore playlistStore, int count, int cooldown, boolean groupPlaylists, String searchString, boolean showSidePanel, boolean showCover, boolean colorBackground) {
        PersistentPreferences preferences = new PersistentPreferences(playlistStore, new MainGuiParams(count, cooldown, groupPlaylists, searchString, showSidePanel, showCover, colorBackground));
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
