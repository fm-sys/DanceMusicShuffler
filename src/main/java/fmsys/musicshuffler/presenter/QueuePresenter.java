package fmsys.musicshuffler.presenter;

import fmsys.musicshuffler.store.PlayerState;
import fmsys.musicshuffler.store.PlayerStore;
import fmsys.musicshuffler.view.QueueView;
import fmsys.musicshuffler.model.TrackWithBadges;
import fmsys.musicshuffler.service.ShuffleAlgorithm;

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
