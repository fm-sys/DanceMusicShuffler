package org.example;

import org.example.models.PlaylistModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaylistStore extends AbstractStore<List<PlaylistModel>> {
    private String filterText = "";

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

    /**
     * Get a collection of playlists which match the current filter.
     */
    public List<PlaylistModel> getFilteredPlaylists() {
        if  (filterText.isBlank()) {
            return get();
        }
        return get().stream().filter(this::matchesFilter).toList();
    }

    private boolean matchesFilter(PlaylistModel playlist) {
        return playlist.getPlaylist().getName().toLowerCase().contains(filterText) ||
                playlist.getPlaylist().getDescription().toLowerCase().contains(filterText) ||
                playlist.getPlaylist().getOwner().getDisplayName().toLowerCase().contains(filterText);
    }

    /**
     * Update the filter text and return whether it has changed. The filter text is normalized to lower case and trimmed.
     */
    public boolean setFilterText(String newFilter) {
        String filterNormalized = newFilter.toLowerCase().trim();
        if (filterNormalized.equals(filterText)) {
            return false;
        }

        filterText = filterNormalized;
        return true;
    }
}
