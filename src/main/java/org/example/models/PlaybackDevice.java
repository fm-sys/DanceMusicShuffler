package org.example.models;

import se.michaelthelin.spotify.model_objects.miscellaneous.Device;

public record PlaybackDevice(String id, String name, boolean isActive) {

    public PlaybackDevice(Device device) {
        this(device.getId(), device.getName(), device.getIs_active());
    }

    @Override
    public String toString() {
        return name;
    }
}