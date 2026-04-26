package org.example;

import org.example.models.PlaybackDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PlaybackDevicesStore {

    private List<PlaybackDevice> devices;
    private final List<Consumer<List<PlaybackDevice>>> listeners = new ArrayList<>();

    public void setState(List<PlaybackDevice> devices) {
        this.devices = devices;
        notifyListeners();
    }

    public List<PlaybackDevice> getDevices() {
        return devices;
    }

    public String getActiveDeviceId() {
        return devices.stream().filter(PlaybackDevice::isActive).findFirst().map(PlaybackDevice::id).orElse(null);
    }

    public void subscribe(Consumer<List<PlaybackDevice>> l) {
        listeners.add(l);
    }

    private void notifyListeners() {
        listeners.forEach(l -> l.accept(devices));
    }
}