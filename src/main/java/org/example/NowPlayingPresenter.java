package org.example;

import org.example.worker.ShuffleAlgorithm;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.specification.Track;

import javax.swing.*;

public class NowPlayingPresenter {

    private final NowPlayingView view;
    private final ShuffleAlgorithm shuffleAlgorithm;

    public NowPlayingPresenter(NowPlayingView view,  ShuffleAlgorithm shuffleAlgorithm) {
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
}
