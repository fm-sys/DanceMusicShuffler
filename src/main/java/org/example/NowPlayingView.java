package org.example;

import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.List;

public interface NowPlayingView {
    void showNowPlaying(Track track, List<String> badges);
}
