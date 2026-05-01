package fmsys.musicshuffler.view;

import fmsys.musicshuffler.model.PlaybackDevice;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.List;

public interface NowPlayingView {
    void showNowPlaying(Track track, List<String> badges);

    void updateDevicesComboBox(java.util.List<PlaybackDevice> devices);
}
