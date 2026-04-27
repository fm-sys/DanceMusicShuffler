package org.example;

import org.example.models.PlaylistModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaylistStore extends AbstractStore<List<PlaylistModel>> {
    public void addPlaylist(PlaylistModel playlistModel) {
        List<PlaylistModel> playlists = new ArrayList<>(get());
        playlists.add(playlistModel);
        setState(Collections.unmodifiableList(playlists));
    }

    /**
     * Get a collection of all checked playlists.
     */
    public List<PlaylistModel> getSelectedPlaylists() {
        return get().stream().filter(PlaylistModel::isChecked).toList();
    }


    @Override
    protected List<PlaylistModel> defaultState() {
        return List.of();
    }
}
