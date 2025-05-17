package org.example.models;

import se.michaelthelin.spotify.model_objects.miscellaneous.Device;

public record DeviceDisplayable(Device device) {
    @Override
    public String toString() {
        return device.getName();
    }
}