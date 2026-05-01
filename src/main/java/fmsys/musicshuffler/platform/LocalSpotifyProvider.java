package fmsys.musicshuffler.platform;

import fmsys.musicshuffler.store.PlayerStore;

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
        boolean isInitialized();
        void initialize(PlayerStore playerStore);
    }

}
