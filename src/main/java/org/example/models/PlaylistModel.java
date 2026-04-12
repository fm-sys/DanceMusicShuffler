package org.example.models;

import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;

import java.util.ArrayList;

public class PlaylistModel {

    private static final boolean DEFAULT_CHECKED = false;
    private static final boolean DEFAULT_EXCLUSIVE = false;
    private static final double DEFAULT_WEIGHT = 1.0;

    private final PlaylistSimplified playlist;
    private boolean checked = DEFAULT_CHECKED;
    private boolean exclusive = DEFAULT_EXCLUSIVE;
    private double weight = DEFAULT_WEIGHT;
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

    /**
     * Set the weight of the playlist. (default: 1.0)
     * The weight is used to determine how often the playlist should be played.
     */
    public void setWeight(double weight) {
        this.weight = weight;
    }
    /**
     * Get the weight of the playlist. (default: 1.0)
     * The weight is used to determine how often the playlist should be played.
     */
    public double getWeight() {
        return weight;
    }


    public void setTracks(ArrayList<PlaylistTrack> tracks) {
        this.tracks = tracks;
    }

    public ArrayList<PlaylistTrack> getTracks() {
        return tracks;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PlaylistModel) {
            return ((PlaylistModel) obj).getPlaylist().getId().equals(playlist.getId());
        }
        return false;
    }

    public boolean hasModifiedConfig() {
        return checked != DEFAULT_CHECKED || exclusive != DEFAULT_EXCLUSIVE || weight != DEFAULT_WEIGHT;
    }
}
