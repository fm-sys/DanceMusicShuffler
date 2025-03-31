package org.example.api;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;

import javax.swing.*;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class Api {

    private static final String clientId = "b7cea7e9e1af4b16b985cd76af7ea846";
    private static final URI redirectUri = SpotifyHttpManager.makeUri("http://127.0.0.1:1702/callback");

    public static final SpotifyApi INSTANCE = new SpotifyApi.Builder()
            .setClientId(clientId)
            .setRedirectUri(redirectUri)
            .build();

    private final String codeVerify = generateCode(128);
    private final String codeChallenge = generateCodeChallenge(codeVerify);
    private final String state = generateCode(16);

    private final AuthorizationCodeUriRequest authorizationCodeUriRequest = INSTANCE.authorizationCodePKCEUri(codeChallenge)
            .state(state)
            .scope("playlist-read-private,user-read-currently-playing,user-read-playback-state,user-modify-playback-state")
//          .show_dialog(true)
            .build();


    private static String generateCode(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder codeChallenge = new StringBuilder(length);
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < length; i++) {
            codeChallenge.append(characters.charAt(random.nextInt(characters.length())));
        }
        return codeChallenge.toString();
    }

    private static String generateCodeChallenge(String codeVerifier) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] hashedBytes = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        return base64UrlEncode(hashedBytes);
    }

    private static String base64UrlEncode(byte[] input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
    }

    private void launchAuthorizationCodeUri() {
        authorizationCodeUriRequest.executeAsync().thenAccept(uri -> {
            try {
                java.awt.Desktop.getDesktop().browse(uri);
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });
    }

    private void serverCallback(String code, Runnable readyCallback) {
        INSTANCE
                .authorizationCodePKCE(code, codeVerify)
                .build()
                .executeAsync()
                .thenAccept(credentials -> {
                    INSTANCE.setAccessToken(credentials.getAccessToken());
                    INSTANCE.setRefreshToken(credentials.getRefreshToken());
                    System.out.println("Autorisation successful. Token expires in: " + credentials.getExpiresIn() + " seconds.");
                    new Timer(1000 * (credentials.getExpiresIn() - 60), evt -> refreshAccessToken()).start();
                    if (readyCallback != null) {
                        readyCallback.run();
                    }
                });
    }

    private void refreshAccessToken() {
        INSTANCE.authorizationCodePKCERefresh()
                .build()
                .executeAsync().thenAccept(credentials -> {
                    INSTANCE.setAccessToken(credentials.getAccessToken());
                    INSTANCE.setRefreshToken(credentials.getRefreshToken());
                    System.out.println("Refreshed token.");
                });
    }

    public void startAuthorizationCodePKCEFlow(Runnable readyCallback) {
        try {
            OAuthRedirectServer.run(state, code -> serverCallback(code, readyCallback));
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
        launchAuthorizationCodeUri();
//        refreshAccessToken();
    }
}
