package org.example;

import org.example.models.PlaylistModel;

import java.util.List;

public interface PlaylistsView {
    void setPlaylistsLists(List<PlaylistModel> playlists);
    void updateSelectAllCheckbox(int selected, int all);
    void setFilterText(String filterText);
}
