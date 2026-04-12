package org.example.api;

import se.michaelthelin.spotify.model_objects.IPlaylistItem;

public class LocalSpotifyProvider {

    public static final LocalSpotifyIntegration INSTANCE;

    static {
        if (com.sun.jna.Platform.isWindows()) {
            INSTANCE = new LocalSpotifyWindows();
        } else {
            INSTANCE = new LocalSpotifyNoop();
        }
    }

    public interface LocalSpotifyIntegration {

        boolean titleHasChanged();
        boolean isPaused();
        boolean isInitialized();
        void initialize(IPlaylistItem currentTrack);
    }

}
