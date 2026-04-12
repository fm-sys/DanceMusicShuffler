package org.example.api;

import se.michaelthelin.spotify.model_objects.IPlaylistItem;

public class LocalSpotifyNoop implements LocalSpotifyProvider.LocalSpotifyIntegration {
    @Override
    public boolean titleHasChanged() {
        return false;
    }

    @Override
    public boolean isPaused() {
        return false;
    }

    @Override
    public boolean isInitialized() {
        return false;
    }

    @Override
    public void initialize(IPlaylistItem currentTrack) {

    }
}
