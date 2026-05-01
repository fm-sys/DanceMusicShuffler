package fmsys.musicshuffler.store;

import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.awt.image.BufferedImage;
import java.util.List;

public record PlayerState(Track track, BufferedImage coverImage, BufferedImage backgroundImage, boolean isPlaying,
                          int progressMs, long timestamp, List<IPlaylistItem> queue) {

    public float getProgressPercentage() {
        if (track == null || track.getDurationMs() <= 0) {
            return 0;
        }

        long durationMs = track.getDurationMs();
        long elapsedMs = isPlaying ? progressMs + (System.currentTimeMillis() - timestamp) : progressMs;
        float progress = (float) elapsedMs / durationMs;

        return Math.max(0f, Math.min(1f, progress));
    }
}
