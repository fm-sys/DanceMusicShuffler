package org.example;

import org.apache.hc.core5.http.ParseException;
import org.example.api.AuthorizationCodePKCEFlow;
import org.example.api.SpotifyWindowTitle;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    static SpotifyApi api;

    static List<PlaylistSimplified> playlists = new ArrayList<>();

    public static void main(String[] args) {

        AuthorizationCodePKCEFlow authorizationCodePKCEFlow = new AuthorizationCodePKCEFlow(Main::initialization);
        api = authorizationCodePKCEFlow.getSpotifyApi();

        if (true) { // you may switch whether to use the authorization code flow or not
            authorizationCodePKCEFlow.start();
        } else {
            api.setAccessToken("???");
            initialization();
        }

    }

    private static void initialization() {
        System.out.println("Authorization code flow finished!");
        System.out.println("Access token: " + api.getAccessToken());

        CurrentlyPlaying currentlyPlaying;
        try {
            currentlyPlaying = api.getUsersCurrentlyPlayingTrack().build().execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            throw new RuntimeException(e);
        }
        SpotifyWindowTitle.searchSpotifyWindowInitial(currentlyPlaying != null ? currentlyPlaying.getItem() : null);

        getPlaylists(() -> new MainGui(playlists, api));
    }

    private static void getPlaylists(Runnable doneCallback) {
        getPlaylists(0, doneCallback);
    }
    private static void getPlaylists(int offset, Runnable doneCallback) {
        // Get a List of Current User's Playlists
        api.getListOfCurrentUsersPlaylists()
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