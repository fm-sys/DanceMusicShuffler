package fmsys.musicshuffler.store;

import fmsys.musicshuffler.model.PlaybackDevice;

import java.util.List;

public class PlaybackDevicesStore extends AbstractStore<List<PlaybackDevice>> {

    public String getActiveDeviceId() {
        return get().stream().filter(PlaybackDevice::isActive).findFirst().map(PlaybackDevice::id).orElse(null);
    }

    @Override
    protected List<PlaybackDevice> defaultState() {
        return List.of();
    }
}