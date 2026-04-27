package org.example.worker;

import org.apache.hc.core5.http.ParseException;
import org.example.PlaylistStore;
import org.example.api.Api;
import org.example.models.PlaylistGroup;
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
    PlaylistStore playlistStore;

    final FixedSizeQueue<PlaylistModel> recentlyUsedPlaylists = new FixedSizeQueue<>(0);
    final List<UsedTrack> alreadyUsedTracks = new ArrayList<>();

    /**
     * whether the last shuffled song was an exclusive one
     */
    boolean wasExclusive = false;


    public ShuffleAlgorithm(PlaylistStore playlistStore) {
        this.playlistStore = playlistStore;
    }

    private UsedTrack getUsedTrackIfExists(IPlaylistItem track) {
        for (UsedTrack usedTrack : alreadyUsedTracks) {
            if (usedTrack.track().getUri().equals(track.getUri())) {
                return usedTrack;
            }
        }
        return null;
    }

    public boolean wasShuffled(IPlaylistItem track) {
        return getUsedTrackIfExists(track) != null;
    }

    public List<String> getBadges(IPlaylistItem track) { // todo: this should not be part of ShuffleAlgorithm, but we can refactor later
        ArrayList<String> badges = new ArrayList<>();

        UsedTrack usedTrack = getUsedTrackIfExists(track);
        if (usedTrack != null) {
            badges.add(usedTrack.from().getPlaylist().getName());
        }

        for (PlaylistModel playlist : playlistStore.getSelectedPlaylists()) {
            if (playlist.getTracks() == null) {
                continue;
            }
            if (usedTrack != null && usedTrack.from().getPlaylist().getId().equals(playlist.getPlaylist().getId())) {
                continue;
            }
            if (playlist.getTracks().stream().anyMatch(playlistTrack -> track.getId().equals(playlistTrack.getItem().getId()))) {
                badges.add(playlist.getPlaylist().getName());
            }
        }
        return badges;
    }

    private ArrayList<PlaylistModel> getAllowedPlaylists(boolean groupPlaylists) {
        ArrayList<PlaylistModel> allowedPlaylists = new ArrayList<>();
        List<PlaylistGroup> groups = groupPlaylists ? PlaylistGroup.createGroups(playlistStore.getSelectedPlaylists()) : null;

        for (PlaylistModel playlist : playlistStore.getSelectedPlaylists()) {
            if ((!wasExclusive || !playlist.isExclusive()) && !isRecentlyUsed(playlist, groups)) {
                allowedPlaylists.add(playlist);
            }
        }

        return allowedPlaylists;
    }

    private boolean isRecentlyUsed(PlaylistModel playlist, List<PlaylistGroup> playlistGroups) {
        if (playlistGroups == null) {
            return recentlyUsedPlaylists.contains(playlist);
        }

        return playlistGroups.stream().filter(group -> group.contains(playlist)) //takes all the groups which the playlist is part of
                .anyMatch(group -> (recentlyUsedPlaylists.stream().anyMatch(group::contains))); //checks if any of those groups contain any of the recently used playlists
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

    public CompletableFuture<Boolean> shuffleAsync(int count, int cooldown, boolean groupPlaylists, String deviceId) {
        return SpotifyApiThreading.executeAsync(() -> shuffle(count, cooldown, groupPlaylists, deviceId));
    }

    public boolean shuffle(int count, int cooldown, boolean groupPlaylists, String deviceId) {
        recentlyUsedPlaylists.setMaxSize(cooldown);

        if (count <= 0) {
            System.out.println("Shuffle finished");
            return true;
        }

        PlaylistModel chosenPlaylist = getWeightedRandom(getAllowedPlaylists(groupPlaylists));
        if (chosenPlaylist == null) {
            System.out.println("No allowed playlists available");
            return false;
        }
        if (chosenPlaylist.getTracks() == null) {
            System.out.println("Playlist tracks not loaded: " + chosenPlaylist.getPlaylist().getName());
            return false;
        }
        List<IPlaylistItem> allowedTracks = chosenPlaylist.getTracks().stream().map(PlaylistTrack::getItem).filter(track -> !wasShuffled(track)).toList();
        if (allowedTracks.isEmpty()) {
            System.out.println("No allowed tracks available in playlist: " + chosenPlaylist.getPlaylist().getName());
            return false;
        }

        wasExclusive = chosenPlaylist.isExclusive();
        IPlaylistItem chosenTrack = allowedTracks.get(new Random().nextInt(allowedTracks.size()));

        recentlyUsedPlaylists.add(chosenPlaylist);
        alreadyUsedTracks.add(new UsedTrack(chosenTrack, chosenPlaylist));

        try {
            Api.INSTANCE.addItemToUsersPlaybackQueue(chosenTrack.getUri()).device_id(deviceId).build().execute();
            System.out.println("Added track to queue: " + chosenTrack.getName());
            return shuffle(count - 1, cooldown, groupPlaylists, deviceId);
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
