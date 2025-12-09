package org.example.models;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PlaylistGroup {
    private final ArrayList<PlaylistModel> playlists;

    public static ArrayList<PlaylistGroup> createGroups(List<PlaylistModel> selectedPlaylists) {
        ArrayList<PlaylistGroup> groups = new ArrayList<>();

        List<PlaylistModel> playlistsSorted = selectedPlaylists.stream()
                .sorted(Comparator.comparingInt(playlist -> playlist.getPlaylist().getName().length()))
                .toList();

        for (PlaylistModel playlist : playlistsSorted) {
            String playlistName = playlist.getPlaylist().getName();
            boolean needsGroup = true;
            for (PlaylistGroup group : groups) {
                if (playlistName.contains(group.getPlaylists().get(0).getPlaylist().getName())) {
                    group.getPlaylists().add(playlist);
                    needsGroup = false;
                }
            }
            if (needsGroup) {
                groups.add(new PlaylistGroup(playlist));
            }
        }
        return groups;
    }

    public PlaylistGroup(PlaylistModel... playlists) {
        this.playlists = new ArrayList<>(List.of(playlists));
    }

    public ArrayList<PlaylistModel> getPlaylists() {
        return playlists;
    }

    public boolean contains(PlaylistModel playlist) {
        return playlists.contains(playlist);
    }

}
