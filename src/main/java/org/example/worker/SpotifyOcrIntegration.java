package org.example.worker;

import org.example.OcrOverlayWindow;

public class SpotifyOcrIntegration {

    private SpotifyOcrWindows windowsImpl;

    public static SpotifyOcrIntegration create(OcrOverlayWindow overlay) {
        if (com.sun.jna.Platform.isWindows()) {
            return new SpotifyOcrIntegration(new SpotifyOcrWindows(overlay));
        } else {
            return new SpotifyOcrIntegration(null);
        }
    }

    private SpotifyOcrIntegration(SpotifyOcrWindows windowsImpl) {
        this.windowsImpl = windowsImpl;
    }

    public void start() {
        if (windowsImpl != null) {
            windowsImpl.start();
        }
    }

    public void stop() {
        if (windowsImpl != null) {
            windowsImpl.stop();
        }
    }

    public boolean isSupported() {
        return windowsImpl != null;
    }
}
