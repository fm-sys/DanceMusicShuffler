package org.example;

import org.example.models.TrackWithBadges;
import org.example.worker.ShuffleAlgorithm;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class QueuePresenter {

    private final QueueView view;
    private final OcrOverlayWindow ocrOverlayWindow;
    private final ShuffleAlgorithm shuffleAlgorithm;

    public QueuePresenter(QueueView view, OcrOverlayWindow ocrOverlayWindow, ShuffleAlgorithm shuffleAlgorithm) {
        this.view = view;
        this.ocrOverlayWindow = ocrOverlayWindow;
        this.shuffleAlgorithm = shuffleAlgorithm;
    }

    public void update(List<IPlaylistItem> queue) {
        List<TrackWithBadges> queueTracks = new ArrayList<>();

        queue.forEach(item -> {
            ArrayList<String> badges = shuffleAlgorithm.getBadges(item);
            queueTracks.add(new TrackWithBadges(item, badges, shuffleAlgorithm.wasShuffled(item)));
        });

        SwingUtilities.invokeLater(() -> {
            view.showQueue(queueTracks);
            ocrOverlayWindow.updateQueue(queueTracks);
        });
    }
}
