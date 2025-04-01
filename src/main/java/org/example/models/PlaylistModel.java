package org.example.models;

import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;

import java.util.ArrayList;

public class PlaylistModel {
    private final PlaylistSimplified playlist;
    private boolean checked = false;
    private boolean exclusive = false;
    private ArrayList<PlaylistTrack> tracks = null;

    public PlaylistModel(PlaylistSimplified playlist) {
        this.playlist = playlist;
    }

    public PlaylistSimplified getPlaylist() {
        return playlist;
    }

    /**
     * Set whether the playlist is part of the general pool of playlists to be played.
     */
    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    /**
     * Check if the playlist is part of the general pool of playlists to be played.
     */
    public boolean isChecked() {
        return checked;
    }

    /**
     * Set whether the playlist is part of the exclusive pool.
     * Playlists in the exclusive pool are not allowed to be played directly after each other.
     */
    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    /**
     * Check if the playlist is part of the exclusive pool.
     * Playlists in the exclusive pool are not allowed to be played directly after each other.
     */
    public boolean isExclusive() {
        return exclusive;
    }

    public void setTracks(ArrayList<PlaylistTrack> tracks) {
        this.tracks = tracks;
    }

    public ArrayList<PlaylistTrack> getTracks() {
        return tracks;
    }
}
