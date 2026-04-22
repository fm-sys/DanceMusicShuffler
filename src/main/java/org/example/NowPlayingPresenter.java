package org.example;

import org.example.models.DeviceDisplayable;
import org.example.worker.ShuffleAlgorithm;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.miscellaneous.Device;
import se.michaelthelin.spotify.model_objects.specification.Track;

import javax.swing.*;
import java.util.ArrayList;
import java.util.stream.Stream;

public class NowPlayingPresenter {

    private final NowPlayingView view;
    private final ShuffleAlgorithm shuffleAlgorithm;

    public NowPlayingPresenter(NowPlayingView view, ShuffleAlgorithm shuffleAlgorithm) {
        this.view = view;
        this.shuffleAlgorithm = shuffleAlgorithm;
    }

    public void update(IPlaylistItem item) {
        SwingUtilities.invokeLater(() -> {
            if (item instanceof Track track) {
                view.showNowPlaying(track, shuffleAlgorithm.getBadges(track));
            } else {
                view.showNowPlaying(null, null);
            }
        });
    }

    public void updatePlaybackDevices(Device[] devices) {
        java.util.List<DeviceDisplayable> devicesList = new ArrayList<>(Stream.of(devices).map(DeviceDisplayable::new).toList());

        devicesList.sort((d1, d2) -> { // "Computers" first, then other devices
            if (d1.device().getType().equals("Computer") && !d2.device().getType().equals("Computer")) {
                return -1;
            } else if (!d1.device().getType().equals("Computer") && d2.device().getType().equals("Computer")) {
                return 1;
            }
            return 0;
        });

        SwingUtilities.invokeLater(() -> view.updateDevicesComboBox(devicesList));
    }
}
