package org.example;

import org.example.models.PlaylistModel;

import java.util.List;

public interface OptionsView {
    void applyPreferences(PreferenceParams params);
    void loadAndShuffleFinished(boolean success);
    void showExclusivePoolDialog(List<PlaylistModel> selectedPlaylists);
    void showWeightsDialog(List<PlaylistModel> selectedPlaylists);
}

