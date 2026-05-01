package fmsys.musicshuffler.view;

import fmsys.musicshuffler.model.TrackWithBadges;

import java.util.List;

public interface QueueView {
    void showQueue(List<TrackWithBadges> tracks);
}
