package org.example;

import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.awt.image.BufferedImage;
import java.util.List;

public record PlayerState (Track track, BufferedImage coverImage, BufferedImage backgroundImage, boolean isPlaying, List<IPlaylistItem> queue) {
}
