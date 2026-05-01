package fmsys.musicshuffler.store;

import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.awt.image.BufferedImage;
import java.util.List;

public record PlayerState(Track track, BufferedImage coverImage, BufferedImage backgroundImage, boolean isPlaying,
                          int progressMs, long timestamp, List<IPlaylistItem> queue) {

    public float getProgressPercentage() {
        if (track == null) {
            return 0;
        }
        if (!isPlaying) {
            return (float) progressMs / track.getDurationMs();
        }
        return (float) (progressMs + (System.currentTimeMillis() - timestamp)) / track.getDurationMs();
    }
}
