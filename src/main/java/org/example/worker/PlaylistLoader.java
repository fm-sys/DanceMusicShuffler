package org.example.worker;

import org.apache.hc.core5.http.ParseException;
import org.example.api.Api;
import org.example.models.PlaylistModel;
import se.michaelthelin.spotify.SpotifyApiThreading;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class PlaylistLoader {
    private PlaylistLoader() {
        // Prevent instantiation
    }

    private static void loadPlaylistsInternal(Collection<PlaylistModel> playlists) throws IOException, ParseException, SpotifyWebApiException {
        for (PlaylistModel playlist : playlists) {
            if (!playlist.isChecked() || playlist.getTracks() != null) {
                continue;
            }

            ArrayList<PlaylistTrack> playlistTracks = new ArrayList<>();

            int offset = 0;
            while (true) {
                Paging<PlaylistTrack> tracks = Api.INSTANCE.getPlaylistsItems(playlist.getPlaylist().getId())
                        .limit(50)
                        .offset(offset)
                        .build()
                        .execute();


                // there are some malformed tracks which we should just skip to don't get more serious problems
                for (PlaylistTrack track : tracks.getItems()) {
                    if (track.getTrack() != null) {
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
     * Playlists which already have their tracks loaded or are not checked will be skipped.
     *
     * @param playlists The collection of PlaylistModel objects to load.
     * @return true if the playlists were loaded successfully, false otherwise.
     */
    public static boolean loadPlaylists(Collection<PlaylistModel> playlists) {
        try {
            loadPlaylistsInternal(playlists);
            return true;
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            return false;
        }
    }

    /**
     * Loads songs of the given playlists asynchronously.
     * Playlists which already have their tracks loaded or are not checked will be skipped.
     *
     * @param playlists The collection of PlaylistModel objects to load.
     * @return A CompletableFuture that will complete with true if the playlists were loaded successfully, false otherwise.
     */
    public static CompletableFuture<Boolean> loadPlaylistsAsync(Collection<PlaylistModel> playlists) {
        return SpotifyApiThreading.executeAsync(() -> loadPlaylists(playlists));
    }
}
