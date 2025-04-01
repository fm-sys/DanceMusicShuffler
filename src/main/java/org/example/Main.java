package org.example;

import org.apache.hc.core5.http.ParseException;
import org.example.api.Api;
import org.example.api.SpotifyWindowTitle;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    static List<PlaylistSimplified> playlists = new ArrayList<>();

    public static void main(String[] args) {


        String accessToken = "";
        if (accessToken == null || accessToken.isEmpty()) {
            new Api().startAuthorizationCodePKCEFlow(Main::initialization);
        } else {
            Api.INSTANCE.setAccessToken(accessToken);
            initialization();
        }
    }

    private static void initialization() {
        System.out.println("Authorization code flow finished!");
        System.out.println("Access token: " + Api.INSTANCE.getAccessToken());

        CurrentlyPlaying currentlyPlaying;
        try {
            currentlyPlaying = Api.INSTANCE.getUsersCurrentlyPlayingTrack().build().execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            throw new RuntimeException(e);
        }
        SpotifyWindowTitle.searchSpotifyWindowInitial(currentlyPlaying != null ? currentlyPlaying.getItem() : null);

        getPlaylists(() -> new MainGui(playlists));
    }

    private static void getPlaylists(Runnable doneCallback) {
        getPlaylists(0, doneCallback);
    }
    private static void getPlaylists(int offset, Runnable doneCallback) {
        // Get a List of Current User's Playlists
        Api.INSTANCE.getListOfCurrentUsersPlaylists()
                .limit(50)
                .offset(offset)
                .build()
                .executeAsync()
                .thenAccept(response -> {
                    playlists.addAll(List.of(response.getItems()));

                    if (response.getTotal() > response.getOffset() + response.getLimit()) {
                        getPlaylists(response.getOffset() + response.getLimit(), doneCallback);
                    } else {
                        System.out.println(playlists.size() + " playlists fetched!");
                        if (doneCallback != null) {
                            doneCallback.run();
                        }
                    }
                });
    }
}