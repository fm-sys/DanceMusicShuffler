package org.example.models;

import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;

public class PlaylistModel {
    private final PlaylistSimplified playlist;
    private boolean checked = false;

    public PlaylistModel(PlaylistSimplified playlist) {
        this.playlist = playlist;
    }

    public PlaylistSimplified getPlaylist() {
        return playlist;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public boolean isChecked() {
        return checked;
    }
}
