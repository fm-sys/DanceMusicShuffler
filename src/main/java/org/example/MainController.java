package org.example;

import com.google.gson.JsonArray;
import org.example.api.Api;
import org.example.api.LocalSpotifyProvider;
import org.example.util.ImageUtils;
import org.example.util.Scheduler;
import org.example.worker.ShuffleAlgorithm;
import se.michaelthelin.spotify.exceptions.detailed.ForbiddenException;
import se.michaelthelin.spotify.model_objects.miscellaneous.Device;
import se.michaelthelin.spotify.model_objects.specification.Track;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;

public class MainController {
    private final QueuePresenter queuePresenter;
    private final NowPlayingPresenter nowPlayingPresenter;

    // todo: they are currently referenced from MainGui, ideally they can be private after refactoring is complete
    SecondaryMonitorGui secondaryMonitorGui = new SecondaryMonitorGui();
    PlaylistStore playlistStore = new PlaylistStore();
    ShuffleAlgorithm shuffleAlgorithm = new ShuffleAlgorithm(playlistStore);
    String activeDeviceId = null;

    public MainController(MainGui mainGui) {
        queuePresenter = new QueuePresenter(mainGui, mainGui.ocrOverlayWindow, shuffleAlgorithm);
        nowPlayingPresenter = new NowPlayingPresenter(mainGui, shuffleAlgorithm);
    }

    /**
     * reloads the queue as well as the currently playing song
     */
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

    /**
     * reloads the currently playing song together with the exact playback position of the track
     */
    public void updatePlaybackState() {
        Api.INSTANCE.getUsersCurrentlyPlayingTrack().build().executeAsync().thenAccept(currentlyPlaying -> {
            if (currentlyPlaying != null) {
                secondaryMonitorGui.setPaused(!currentlyPlaying.getIs_playing());
                secondaryMonitorGui.setProgress(currentlyPlaying.getProgress_ms(), currentlyPlaying.getItem().getDurationMs());
            }
        }).whenComplete((res, ex) -> {
            if (ex != null) {
                System.err.println("Caught Exception: " + ex.getMessage());
            }
        });
    }

    public void playPauseClicked() {
        Api.INSTANCE.startResumeUsersPlayback().device_id(activeDeviceId).build().executeAsync().whenComplete((res, ex) -> {
            if (ex instanceof ForbiddenException) {
                // play not possible when already playing, perform pause instead
                Api.INSTANCE.pauseUsersPlayback().build().executeAsync().whenComplete((res2, ex2) -> secondaryMonitorGui.setPaused(true));
            } else if (ex != null) {
                System.err.println("startResumeUsersPlayback: Caught Exception: " + ex.getMessage());
            } else {
                Scheduler.waitForWebApiDelayAndRun(this::updatePlaybackState);
            }
        });
    }

    public void skipToNextClicked() {
        Api.INSTANCE.skipUsersPlaybackToNextTrack().build().executeAsync().whenComplete((res, ex) -> {
            if (ex != null) {
                System.err.println("skipUsersPlaybackToNextTrack: Caught Exception: " + ex.getMessage());
            } else {
                secondaryMonitorGui.setPaused(false);
            }
        });
    }

    public void refreshAvailablePlaybackDevices() {
        Api.INSTANCE.getUsersAvailableDevices().build().executeAsync().whenComplete((res, ex) -> {
            if (ex != null) {
                System.err.println("getUsersAvailableDevices: Caught Exception: " + ex.getMessage());
            } else {
                nowPlayingPresenter.updatePlaybackDevices(res);
            }
        });
    }

    public void transferPlaybackToDevice(Device device) {
        activeDeviceId = device.getId();

        if (device.getIs_active()) {
            System.out.println("Device " + device.getName() + " was already active.");
            return;
        }

        JsonArray array = new JsonArray();
        array.add(device.getId());

        Api.INSTANCE.transferUsersPlayback(array).build().executeAsync().whenComplete((res, ex) -> {
            if (ex == null) {
                System.out.println("Transferred playback to device: " + device.getName());
            } else {
                System.out.println("Selected device: " + device.getName());
                System.err.println("Caught Exception: " + ex.getMessage());
            }
        });
    }

}
