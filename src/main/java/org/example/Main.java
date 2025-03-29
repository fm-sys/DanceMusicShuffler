package org.example;

import org.example.api.AuthorizationCodePKCEFlow;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;

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

        getPlaylists(() -> new MainGui(playlists));
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
                    System.out.println("Total: " + response.getTotal() + " Current offset: " + response.getOffset());
                    playlists.addAll(List.of(response.getItems()));

                    if (response.getTotal() > response.getOffset() + response.getLimit()) {
                        getPlaylists(response.getOffset() + response.getLimit(), doneCallback);
                    } else {
                        System.out.println("All playlists fetched!");
                        if (doneCallback != null) {
                            doneCallback.run();
                        }
                    }
                });
    }
}