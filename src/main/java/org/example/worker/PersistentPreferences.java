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
    List<PersistentPlaylistModel> playlists = null;

    public PersistentPreferences() {
        // Default constructor for Jackson
    }

    public PersistentPreferences(List<PlaylistModel> playlists, int count, int cooldown) {
        this.playlists = playlists.stream().map(pl -> new PersistentPlaylistModel(pl.getPlaylist().getId(), pl.isChecked(), pl.isExclusive(), pl.getWeight())).toList();
        this.count = count;
        this.cooldown = cooldown;
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

        public MainGuiParams(int count, int cooldown) {
            this.count = count;
            this.cooldown = cooldown;
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
            return new MainGuiParams(preferences.count, preferences.cooldown);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void store(List<PlaylistModel> playlists, int count, int cooldown) {
        PersistentPreferences preferences = new PersistentPreferences(playlists, count, cooldown);
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
