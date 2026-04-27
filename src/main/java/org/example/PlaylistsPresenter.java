package org.example;

import org.example.models.PlaylistModel;

import javax.swing.*;
import java.util.List;

public class PlaylistsPresenter {

    private PlaylistsView view;

    PlaylistStore playlistStore;

    public PlaylistsPresenter(PlaylistStore playlistStore) {
        this.playlistStore = playlistStore;
    }

    public void init(PlaylistsView view) {
        this.view = view;

        playlistStore.subscribe(this::update);
        playlistStore.filterText().subscribe(this::updateFilter);
    }

    private void updateFilter(String filterText) {
        SwingUtilities.invokeLater(() -> view.setFilterText(filterText));
    }

    private void update(List<PlaylistModel> playlistModels) {
        SwingUtilities.invokeLater(() -> view.setPlaylistsLists(playlistStore.getFilteredPlaylists()));
        updateSelectAllCheckbox();
    }

    public void filterTextChanged(String newFilter) {
        playlistStore.filterText().setState(newFilter);
    }

    public void selectAllCheckboxClicked(boolean isSelected) {
        playlistStore.getFilteredPlaylists().forEach(playlist -> playlist.setChecked(isSelected));
        updateSelectAllCheckbox();

        playlistStore.notifyListeners();
    }

    public void playlistCheckboxClicked(PlaylistModel playlist, boolean isChecked) {
        playlist.setChecked(isChecked);
        updateSelectAllCheckbox();

        //playlistStore.notifyListeners(); probably we don't need to call listeners?
    }

    private void updateSelectAllCheckbox() {
        List<PlaylistModel> playlistsFiltered = playlistStore.getFilteredPlaylists();
        int selectedCount = (int) playlistsFiltered.stream().filter(PlaylistModel::isChecked).count();
        SwingUtilities.invokeLater(() -> view.updateSelectAllCheckbox(selectedCount, playlistsFiltered.size()));
    }
}
