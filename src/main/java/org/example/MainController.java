package org.example;

import org.example.api.Api;
import org.example.api.LocalSpotifyProvider;
import org.example.util.ImageUtils;
import org.example.worker.ShuffleAlgorithm;
import se.michaelthelin.spotify.model_objects.specification.Track;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;

public class MainController {
    private final QueuePresenter  queuePresenter;
    private final NowPlayingPresenter  nowPlayingPresenter;

    // todo: they are currently referenced from MainGui, ideally they can be private after refactoring is complete
    SecondaryMonitorGui secondaryMonitorGui = new SecondaryMonitorGui();
    PlaylistStore playlistStore = new PlaylistStore();
    ShuffleAlgorithm shuffleAlgorithm = new ShuffleAlgorithm(playlistStore);

    public MainController(MainGui mainGui) {
        queuePresenter = new QueuePresenter(mainGui, mainGui.ocrOverlayWindow, shuffleAlgorithm);
        nowPlayingPresenter = new NowPlayingPresenter(mainGui, shuffleAlgorithm);
    }

    public void refreshPlayerState() {
        Api.INSTANCE.getTheUsersQueue().build().executeAsync().thenAccept(response -> {
            if (!LocalSpotifyProvider.INSTANCE.isInitialized()) {
                LocalSpotifyProvider.INSTANCE.initialize(response.getCurrentlyPlaying());
            }

            queuePresenter.update(response.getQueue());
            nowPlayingPresenter.update(response.getCurrentlyPlaying());

            //todo: create own secondary monitor presenter and move this logic there
            if (response.getCurrentlyPlaying() != null && response.getCurrentlyPlaying() instanceof Track track) {
                if (track.getAlbum() != null && track.getAlbum().getImages() != null) {
                    Arrays.stream(track.getAlbum().getImages()).findFirst().ifPresent(image -> new Thread(() -> {
                        try {
                            URI url = new URI(image.getUrl());
                            BufferedImage rawImage = ImageIO.read(url.toURL());

                            BufferedImage coverImage = ImageUtils.makeRoundedCorner(rawImage, 50);
                            BufferedImage backgroundImage = ImageUtils.dimImage(ImageUtils.blurWithEdgeExtension(rawImage, 150), 0.5f);

                            secondaryMonitorGui.update(coverImage, backgroundImage, track, shuffleAlgorithm.getBadges(track));

                            java.util.List<java.util.List<String>> nextPlaylists = new ArrayList<>();
                            response.getQueue().forEach(item -> nextPlaylists.add(shuffleAlgorithm.getBadges(item)));
                            secondaryMonitorGui.updateSidePanel(nextPlaylists.stream().limit(5).toList());
                        } catch (Exception exp) {
                            exp.printStackTrace();
                        }
                    }).start());
                }

            }

        }).whenComplete((res, ex) -> {
            if (ex != null) {
                System.err.println("Caught Exception: " + ex.getMessage());
            }
        });

    }
}
