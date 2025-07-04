package org.example.worker;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.models.PlaylistModel;

import java.io.File;
import java.io.IOException;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PersistentPreferences {

    int count = 0;
    int cooldown = 0;
    String searchString = null;
    boolean showSidePanel = true;
    boolean colorBackground = true;
    List<PersistentPlaylistModel> playlists = null;

    public PersistentPreferences() {
        // Default constructor for Jackson
    }

    public PersistentPreferences(List<PlaylistModel> playlists, MainGuiParams params) {
        this.playlists = playlists.stream().map(pl -> new PersistentPlaylistModel(pl.getPlaylist().getId(), pl.isChecked(), pl.isExclusive(), pl.getWeight())).toList();
        this.count = params.count;
        this.cooldown = params.cooldown;
        this.searchString = params.searchString;
        this.showSidePanel = params.showSidePanel;
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
        public String searchString;
        public boolean showSidePanel;
        public boolean colorBackground;

        public MainGuiParams(int count, int cooldown, String searchString, boolean showSidePanel, boolean colorBackground) {
            this.count = count;
            this.cooldown = cooldown;
            this.searchString = searchString;
            this.showSidePanel = showSidePanel;
            this.colorBackground = colorBackground;
        }
    }

    /**
     * Load the persistent preferences from a file.
     * The file is expected to be in JSON format.
     *
     * @param playlists The playlist objects to be enriched.
     */
    public static MainGuiParams load(List<PlaylistModel> playlists) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // Read JSON from a file and convert it to a Java object
            PersistentPreferences preferences = objectMapper.readValue(new File("prefs.json"), PersistentPreferences.class);

            // Enrich the playlists with the loaded preferences
            for (PlaylistModel playlist : playlists) {
                for (PersistentPlaylistModel persistentPlaylist : preferences.playlists) {
                    if (playlist.getPlaylist().getId().equals(persistentPlaylist.id)) {
                        playlist.setChecked(persistentPlaylist.checked);
                        playlist.setExclusive(persistentPlaylist.exclusive);
                        playlist.setWeight(persistentPlaylist.weight);
                    }
                }
            }
            return new MainGuiParams(preferences.count, preferences.cooldown, preferences.searchString, preferences.showSidePanel, preferences.colorBackground);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void store(List<PlaylistModel> playlists, int count, int cooldown, String searchString, boolean showSidePanel, boolean colorBackground) {
        PersistentPreferences preferences = new PersistentPreferences(playlists, new MainGuiParams(count, cooldown, searchString, showSidePanel, colorBackground));
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
