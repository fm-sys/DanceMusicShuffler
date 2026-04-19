package org.example;

import org.example.models.TrackWithBadges;

import java.util.List;

public interface QueueView {
    void showQueue(List<TrackWithBadges> tracks);
}
