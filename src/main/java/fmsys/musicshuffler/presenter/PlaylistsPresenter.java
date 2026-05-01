package fmsys.musicshuffler.presenter;

import fmsys.musicshuffler.store.FilterStore;
import fmsys.musicshuffler.store.PlaylistStore;
import fmsys.musicshuffler.view.PlaylistsView;
import fmsys.musicshuffler.model.PlaylistModel;

import javax.swing.*;
import java.util.List;

public class PlaylistsPresenter {

    private PlaylistsView view;

    PlaylistStore playlistStore;
    FilterStore filterStore;

    public PlaylistsPresenter(PlaylistStore playlistStore, FilterStore filterStore) {
        this.playlistStore = playlistStore;
        this.filterStore = filterStore;
    }

    public void init(PlaylistsView view) {
        this.view = view;

        playlistStore.subscribe(ignored -> update());
        filterStore.subscribe(this::updateFilter);
    }

    private void updateFilter(String filterText) {
        SwingUtilities.invokeLater(() -> view.setFilterText(filterText));
        update();
    }

    private void update() {
        SwingUtilities.invokeLater(() -> view.setPlaylistsLists(getFilteredPlaylists()));
        updateSelectAllCheckbox();
    }

    public void filterTextChanged(String newFilter) {
        filterStore.setState(newFilter);
    }

    public void selectAllCheckboxClicked(boolean isSelected) {
        getFilteredPlaylists().forEach(playlist -> playlist.setChecked(isSelected));
        update();
    }

    public void playlistCheckboxClicked(PlaylistModel playlist, boolean isChecked) {
        playlist.setChecked(isChecked);
        updateSelectAllCheckbox();
    }

    private void updateSelectAllCheckbox() {
        List<PlaylistModel> playlistsFiltered = getFilteredPlaylists();
        int selectedCount = (int) playlistsFiltered.stream().filter(PlaylistModel::isChecked).count();
        SwingUtilities.invokeLater(() -> view.updateSelectAllCheckbox(selectedCount, playlistsFiltered.size()));
    }

    private List<PlaylistModel> getFilteredPlaylists() {
        String filter = filterStore.normalized();
        if (filter.isBlank()) {
            return playlistStore.get();
        }
        return playlistStore.get().stream().filter(playlist ->
                playlist.getPlaylist().getName().toLowerCase().contains(filter)
                        || playlist.getPlaylist().getDescription().toLowerCase().contains(filter)
                        || playlist.getPlaylist().getOwner().getDisplayName().toLowerCase().contains(filter)
        ).toList();
    }
}
