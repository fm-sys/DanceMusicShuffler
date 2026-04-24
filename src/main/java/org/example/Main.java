package org.example;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.apache.hc.core5.http.ParseException;
import org.example.api.Api;
import org.example.api.LocalSpotifyProvider;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;

import java.io.IOException;
import java.util.*;

public class Main {

    static List<PlaylistSimplified> playlists = new ArrayList<>();

    public static void main(String[] args) {
        Map<String, String> overrides = new HashMap<>();
        overrides.put("@background", "#26282b");
        overrides.put("@accentColor", "#1ed760");
        overrides.put("@accentFocusColor", "darken(@accentColor,30%)");
        overrides.put("CheckBox.icon.style", "filled");
        overrides.put("CheckBox.icon.checkmarkColor", "@accentColor");
        overrides.put("TextComponent.arc", "24");
        FlatLaf.setGlobalExtraDefaults(overrides);
        FlatDarkLaf.setup();

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

        SplashScreen splashScreen = new SplashScreen();

        CurrentlyPlaying currentlyPlaying;
        try {
            currentlyPlaying = Api.INSTANCE.getUsersCurrentlyPlayingTrack().build().execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        LocalSpotifyProvider.INSTANCE.initialize(currentlyPlaying != null ? currentlyPlaying.getItem() : null);

        getPlaylists(() -> {
            splashScreen.dispose();
            new MainGui(playlists);
        });
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
                }).whenComplete((res, ex) -> {
                    if (ex != null) {
                        System.err.println("Caught Exception: " + ex.getMessage());
                    }
                });
    }
}