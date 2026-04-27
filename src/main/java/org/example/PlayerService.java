package org.example;

import com.google.gson.JsonArray;
import org.example.api.Api;
import org.example.models.PlaybackDevice;
import org.example.util.ImageUtils;
import org.example.util.Scheduler;
import se.michaelthelin.spotify.exceptions.detailed.ForbiddenException;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.model_objects.miscellaneous.Device;
import se.michaelthelin.spotify.model_objects.special.PlaybackQueue;
import se.michaelthelin.spotify.model_objects.specification.Track;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class PlayerService {

    public final PlayerStore playerStore;
    public final PlaybackDevicesStore playbackDevicesStore;

    private record MergedPlayback(Track currentTrack, boolean isPlaying, List<IPlaylistItem> queue) {}
    private record CoverImage(BufferedImage coverImage, BufferedImage backgroundImage) {}


    PlayerService(PlayerStore playerStore,  PlaybackDevicesStore playbackDevicesStore) {
        this.playerStore = playerStore;
        this.playbackDevicesStore = playbackDevicesStore;
    }

    private CompletableFuture<CoverImage> loadCoverImages(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BufferedImage cover = ImageIO.read(new URI(url).toURL());

                return new CoverImage(
                        ImageUtils.makeRoundedCorner(cover, 50),
                        ImageUtils.dimImage(ImageUtils.blurWithEdgeExtension(cover, 150), 0.5f)
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private CompletableFuture<CoverImage> resolveCover(Track track) {
        if (track == null) {
            return CompletableFuture.completedFuture(null);
        }

        if (track.getAlbum() == null || track.getAlbum().getImages() == null) {
            return CompletableFuture.completedFuture(null);
        }

        return Arrays.stream(track.getAlbum().getImages())
                .findFirst()
                .map(img -> loadCoverImages(img.getUrl()))
                .orElseGet(() -> CompletableFuture.completedFuture(null));
    }

    private static boolean sameTrack(IPlaylistItem a, IPlaylistItem b) {
        if (a == null || b == null) {
            return false;
        }
        return a.getId().equals(b.getId());
    }

    private MergedPlayback mergePlayback(PlaybackQueue queueResponse, CurrentlyPlaying currentlyPlayingResponse) {
        Track queueCurrent = (queueResponse.getCurrentlyPlaying() instanceof Track t) ? t : null;
        Track currentlyPlaying = (currentlyPlayingResponse != null && currentlyPlayingResponse.getItem() instanceof Track t)
                ? t
                : null;

        boolean isPlaying = currentlyPlayingResponse != null && Boolean.TRUE.equals(currentlyPlayingResponse.getIs_playing());

        List<IPlaylistItem> queueItems = new ArrayList<>();
        if (queueResponse.getQueue() != null) {
            queueItems.addAll(queueResponse.getQueue());
        }

        // Resolve duplicated currently-playing entry in the queue when queue/currently-playing APIs are briefly out of sync.
        if (!sameTrack(queueCurrent, currentlyPlaying)
                && !queueItems.isEmpty()
                && sameTrack(queueItems.getFirst(), currentlyPlaying)) {
            queueItems.removeFirst();
        }

        Track effectiveCurrent = currentlyPlaying != null ? currentlyPlaying : queueCurrent;
        return new MergedPlayback(effectiveCurrent, isPlaying, queueItems);
    }

    /**
     * reloads the queue as well as the currently playing song
     */
    public void refreshPlayerState() {
        System.out.println("Refreshing playback state");

        CompletableFuture<PlaybackQueue> queueFuture = Api.INSTANCE.getTheUsersQueue()
                .build()
                .executeAsync();

        CompletableFuture<CurrentlyPlaying> currentlyPlayingFuture = Api.INSTANCE.getUsersCurrentlyPlayingTrack()
                .build()
                .executeAsync();

        queueFuture
                .thenCombine(currentlyPlayingFuture, this::mergePlayback)
                .thenCompose(merged ->
                        resolveCover(merged.currentTrack())
                                .thenApply(cover -> new PlayerState(
                                        merged.currentTrack(),
                                        cover != null ? cover.coverImage() : null,
                                        cover != null ? cover.backgroundImage() : null,
                                        merged.isPlaying(),
                                        Collections.unmodifiableList(merged.queue())
                                ))
                )
                .thenAccept(playerStore::setState)
                .exceptionally(ex -> {
                    System.err.println("Caught Exception: " + ex.getMessage());
                    return null;
                });
    }


    public void togglePlayback() {
        Api.INSTANCE.startResumeUsersPlayback().device_id(playbackDevicesStore.getActiveDeviceId()).build().executeAsync().whenComplete((res, ex) -> {
            if (ex instanceof ForbiddenException) {
                // play not possible when already playing, perform pause instead
                Api.INSTANCE.pauseUsersPlayback().build().executeAsync().whenComplete((res2, ex2) -> Scheduler.waitForWebApiDelayAndRun(this::refreshPlayerState));
            } else if (ex != null) {
                System.err.println("startResumeUsersPlayback: Caught Exception: " + ex.getMessage());
            } else {
                Scheduler.waitForWebApiDelayAndRun(this::refreshPlayerState);
            }
        });
    }

    public void skipToNext() {
        Api.INSTANCE.skipUsersPlaybackToNextTrack().device_id(playbackDevicesStore.getActiveDeviceId()).build().executeAsync().whenComplete((res, ex) -> {
            if (ex != null) {
                System.err.println("skipUsersPlaybackToNextTrack: Caught Exception: " + ex.getMessage());
            } else {
                Scheduler.waitForWebApiDelayAndRun(this::refreshPlayerState);
            }
        });
    }

    public void restartCurrent() {
        Api.INSTANCE.seekToPositionInCurrentlyPlayingTrack(0).device_id(playbackDevicesStore.getActiveDeviceId()).build().executeAsync().whenComplete((res, ex) -> {
            if (ex != null) {
                System.err.println("seekToPositionInCurrentlyPlayingTrack: Caught Exception: " + ex.getMessage());
            } else {
                Api.INSTANCE.startResumeUsersPlayback().device_id(playbackDevicesStore.getActiveDeviceId()).build().executeAsync().thenAccept(res2 -> Scheduler.waitForWebApiDelayAndRun(this::refreshPlayerState));
            }
        });
    }

    public void refreshAvailablePlaybackDevices() {
        Api.INSTANCE.getUsersAvailableDevices().build().executeAsync().whenComplete((res, ex) -> {
            if (ex != null) {
                System.err.println("getUsersAvailableDevices: Caught Exception: " + ex.getMessage());
            } else {
                updatePlaybackDevices(res);
            }
        });
    }

    private void updatePlaybackDevices(Device[] devices) {
        List<Device> devicesList = new ArrayList<>(Stream.of(devices).toList());

        devicesList.sort((d1, d2) -> { // "Computers" first, then other devices
            if (d1.getType().equals("Computer") && !d2.getType().equals("Computer")) {
                return -1;
            } else if (!d1.getType().equals("Computer") && d2.getType().equals("Computer")) {
                return 1;
            }
            return 0;
        });

        playbackDevicesStore.setState(devicesList.stream().map(PlaybackDevice::new).toList());
    }

    public void transferPlaybackToDevice(PlaybackDevice device) {
        if (device.isActive()) {
            System.out.println("Device " + device.name() + " was already active.");
            return;
        }

        playbackDevicesStore.setState(playbackDevicesStore.get().stream()
                .map(d -> new PlaybackDevice(d.id(), d.name(), d.id().equals(device.id())))
                .toList());

        JsonArray array = new JsonArray();
        array.add(device.id());

        Api.INSTANCE.transferUsersPlayback(array).build().executeAsync().whenComplete((res, ex) -> {
            if (ex == null) {
                System.out.println("Transferred playback to device: " + device.name());
            } else {
                System.out.println("Selected device: " + device.name());
                System.err.println("Caught Exception: " + ex.getMessage());
            }
        });
    }
}