package org.example.worker;

import org.apache.hc.core5.http.ParseException;
import org.example.api.Api;
import org.example.models.PlaylistModel;
import org.example.models.UsedTrack;
import org.example.util.FixedSizeQueue;
import se.michaelthelin.spotify.SpotifyApiThreading;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ShuffleAlgorithm {
    ArrayList<PlaylistModel> playlists;

    final FixedSizeQueue<PlaylistModel> recentlyUsedPlaylists = new FixedSizeQueue<>(0);
    final ArrayList<UsedTrack> alreadyUsedTracks = new ArrayList<>();

    boolean wasExclusive = false;


    public ShuffleAlgorithm(ArrayList<PlaylistModel> playlists) {
        this.playlists = playlists;
    }

    public UsedTrack getUsedTrackIfExists(IPlaylistItem track) {
        for (UsedTrack usedTrack : alreadyUsedTracks) {
            if (usedTrack.track().getUri().equals(track.getUri())) {
                return usedTrack;
            }
        }
        return null;
    }

    public boolean wasNotShuffled(IPlaylistItem track) {
        return getUsedTrackIfExists(track) == null;
    }

    private ArrayList<PlaylistModel> getAllowedPlaylists() {
        ArrayList<PlaylistModel> allowedPlaylists = new ArrayList<>();

        for (PlaylistModel playlist : playlists) {
            if (playlist.isChecked() && (!wasExclusive || !playlist.isExclusive()) && !recentlyUsedPlaylists.contains(playlist)) {
                allowedPlaylists.add(playlist);
            }
        }

        return allowedPlaylists;
    }

    private PlaylistModel getWeightedRandom(List<PlaylistModel> items) {
        if (items.isEmpty()) {
            return null;
        }

        Random random = new Random();
        double totalWeight = items.stream().mapToDouble(PlaylistModel::getWeight).sum();
        double randomValue = random.nextDouble() * totalWeight; // Random-Wert zwischen 0 und totalWeight

        double cumulativeWeight = 0.0;
        for (PlaylistModel item : items) {
            cumulativeWeight += item.getWeight();
            if (randomValue < cumulativeWeight) {
                return item;
            }
        }
        throw new RuntimeException("We failed at random"); // Sollte nie passieren
    }

    public CompletableFuture<Boolean> shuffleAsync(int count, int cooldown, String deviceId) {
        return SpotifyApiThreading.executeAsync(() -> shuffle(count, cooldown, deviceId));
    }

    public boolean shuffle(int count, int cooldown, String deviceId) {
        recentlyUsedPlaylists.setMaxSize(cooldown);

        if (count <= 0) {
            System.out.println("Shuffle finished");
            return true;
        }

        PlaylistModel chosenPlaylist = getWeightedRandom(getAllowedPlaylists());
        if (chosenPlaylist == null) {
            System.out.println("No allowed playlists available");
            return false;
        }
        wasExclusive = chosenPlaylist.isExclusive();

        List<IPlaylistItem> allowedTracks = chosenPlaylist.getTracks().stream().map(PlaylistTrack::getTrack).filter(this::wasNotShuffled).toList();
        if (allowedTracks.isEmpty()) {
            System.out.println("No allowed tracks available in playlist: " + chosenPlaylist.getPlaylist().getName());
            return false;
        }

        IPlaylistItem chosenTrack = allowedTracks.get(new Random().nextInt(allowedTracks.size()));

        recentlyUsedPlaylists.add(chosenPlaylist);
        alreadyUsedTracks.add(new UsedTrack(chosenTrack, chosenPlaylist));

        try {
            Api.INSTANCE.addItemToUsersPlaybackQueue(chosenTrack.getUri()).device_id(deviceId).build().execute();
            System.out.println("Added track to queue: " + chosenTrack.getName());
            return shuffle(count - 1, cooldown, deviceId);
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
