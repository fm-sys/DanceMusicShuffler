package org.example.api;

import org.example.PlayerStore;

public class LocalSpotifyNoop implements LocalSpotifyProvider.LocalSpotifyIntegration {
    @Override
    public boolean titleHasChanged() {
        return false;
    }

    @Override
    public boolean isInitialized() {
        return false;
    }

    @Override
    public void initialize(PlayerStore playerStore) {

    }
}
