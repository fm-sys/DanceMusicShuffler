package org.example;

import org.example.models.PlaylistModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaylistStore extends AbstractStore<List<PlaylistModel>> {

    public class FilterStore extends AbstractStore<String> {
        @Override
        protected String defaultState() {
            return "";
        }

        @Override
        public void setState(String newState) {
            if (newState == null || FilterStore.this.get().equals(newState)) {
                return;
            }
            boolean isNew = !normalized().equals(newState.toLowerCase().trim());
            super.setState(newState);
            if (isNew) {
                PlaylistStore.this.notifyListeners();
            }
        }

        private String normalized() {
            return FilterStore.this.get().toLowerCase().trim();
        }
    }

    private final FilterStore filterStore = new FilterStore();

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
        if  (filterStore.normalized().isBlank()) {
            return get();
        }
        return get().stream().filter(this::matchesFilter).toList();
    }

    private boolean matchesFilter(PlaylistModel playlist) {
        String filter = filterStore.normalized();
        return playlist.getPlaylist().getName().toLowerCase().contains(filter) ||
                playlist.getPlaylist().getDescription().toLowerCase().contains(filter) ||
                playlist.getPlaylist().getOwner().getDisplayName().toLowerCase().contains(filter);
    }

    public FilterStore filterText() {
        return filterStore;
    }

    @Override
    protected List<PlaylistModel> defaultState() {
        return List.of();
    }
}
