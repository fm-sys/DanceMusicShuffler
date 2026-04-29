package org.example;

import org.example.models.TrackWithBadges;
import org.example.worker.ShuffleAlgorithm;

import javax.swing.*;
import java.util.List;

public class QueuePresenter {

    private QueueView view;

    private final ShuffleAlgorithm shuffleAlgorithm;
    private final PlayerStore playerStore;

    public QueuePresenter(PlayerStore playerStore, ShuffleAlgorithm shuffleAlgorithm) {
        this.shuffleAlgorithm = shuffleAlgorithm;
        this.playerStore = playerStore;
    }

    public void init(QueueView view) {
        this.view = view;

        playerStore.subscribe(this::update);
    }

    public void update(PlayerState state) {
        List<TrackWithBadges> queueTracks = state.queue().stream().map(item -> {
            List<String> badges = shuffleAlgorithm.getBadges(item);
            return new TrackWithBadges(item, badges, shuffleAlgorithm.wasShuffled(item));
        }).toList();

        SwingUtilities.invokeLater(() -> view.showQueue(queueTracks));
    }
}
