package org.example.worker;

import org.apache.hc.core5.http.ParseException;
import org.example.PlaylistStore;
import org.example.api.Api;
import org.example.models.PlaylistModel;
import se.michaelthelin.spotify.SpotifyApiThreading;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class PlaylistLoader {
    private PlaylistLoader() {
        // Prevent instantiation
    }

    public static void loadPlaylistModelFromPrefs(PersistentPreferences.PersistentPlaylistModel loadFrom, PlaylistStore playlistStore) {
        try {
            PlaylistModel playlistModel = new PlaylistModel(Api.INSTANCE.getPlaylist(loadFrom.id).build().execute());
            playlistModel.setChecked(loadFrom.checked);
            playlistModel.setExclusive(loadFrom.exclusive);
            playlistModel.setWeight(loadFrom.weight);
            playlistModel.setFromConfig(true);
            SwingUtilities.invokeLater(() -> playlistStore.addPlaylist(playlistModel));
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Failed to load playlist with id " + loadFrom.id + ": " + e.getMessage());
        }
    }

    private static void loadPlaylistItemsInternal(Collection<PlaylistModel> playlists) throws IOException, ParseException, SpotifyWebApiException {
        for (PlaylistModel playlist : playlists) {
            if (playlist.getTracks() != null) {
                continue;
            }

            System.out.println("Loading tracks for playlist " + playlist.getPlaylist().getName() + "...");

            ArrayList<PlaylistTrack> playlistTracks = new ArrayList<>();
            int offset = 0;

            if (playlist.getPlaylist() instanceof Playlist p && p.getItems() != null) {
                for (PlaylistTrack track : p.getItems().getItems()) {
                    if (track.getItem() != null) {
                        playlistTracks.add(track);
                    }
                }
                if (p.getItems().getNext() == null) {
                    playlist.setTracks(playlistTracks);
                    continue;
                }
                offset += p.getItems().getLimit();
            }

            while (true) {
                Paging<PlaylistTrack> tracks = Api.INSTANCE.getPlaylistsItems(playlist.getPlaylist().getId())
                        .offset(offset)
                        .build()
                        .execute();


                // there are some malformed tracks which we should just skip to don't get more serious problems
                for (PlaylistTrack track : tracks.getItems()) {
                    if (track.getItem() != null) {
                        playlistTracks.add(track);
                    }
                }

                if (tracks.getNext() == null) {
                    break;
                }
                offset += tracks.getLimit();
            }
            playlist.setTracks(playlistTracks);
        }
    }

    /**
     * Loads songs of the given playlists synchronously.
     * Playlists which already have their tracks loaded will be skipped.
     *
     * @param playlists The collection of PlaylistModel objects to load.
     * @return true if the playlists were loaded successfully, false otherwise.
     */
    public static boolean loadPlaylists(Collection<PlaylistModel> playlists) {
        try {
            loadPlaylistItemsInternal(playlists);
            return true;
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Loads songs of the given playlists asynchronously.
     * Playlists which already have their tracks loaded will be skipped.
     *
     * @param playlists The collection of PlaylistModel objects to load.
     * @return A CompletableFuture that will complete with true if the playlists were loaded successfully, false otherwise.
     */
    public static CompletableFuture<Boolean> loadPlaylistsAsync(Collection<PlaylistModel> playlists) {
        return SpotifyApiThreading.executeAsync(() -> loadPlaylists(playlists));
    }
}
