package org.example.api;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;

public class SpotifyWindowTitle {
    static HWND spotifyWindow = null;
    static String lastWindowTitle = null;

    public static void main(String[] args) {
        searchSpotifyWindowInitial(null);

        while (spotifyWindow != null) {
            if (titleChanged()) {
                System.out.println("Spotify window title changed: " + lastWindowTitle);
            }
        }

    }

    public static void searchSpotifyWindowInitial(IPlaylistItem currentTrack) {
        // Enumerate all top-level windows
        User32.INSTANCE.EnumWindows((hWnd, arg1) -> {
            // Get the window title length
            String wText = getWText(hWnd);

            // If the title is empty, skip it
            if (wText.isEmpty()) {
                return true;
            }

            // Check if this window belongs to Spotify
            if (wText.equalsIgnoreCase("Spotify Premium") || (currentTrack != null && wText.contains(currentTrack.getName()))) {
                System.out.println("Found Spotify window: " + wText);
                spotifyWindow = hWnd;
                lastWindowTitle = wText;
            }
            return true; // Continue enumeration
        }, null);
    }

    private static String getWText(HWND hWnd) {
        int titleLength = User32.INSTANCE.GetWindowTextLength(hWnd) + 1;
        char[] windowText = new char[titleLength];
        User32.INSTANCE.GetWindowText(hWnd, windowText, titleLength);
        return Native.toString(windowText).trim();
    }

    public static boolean titleChanged() {
        if (spotifyWindow == null) {
            return false;
        }

        String newTitle = getWText(spotifyWindow);
        if (newTitle.equals(lastWindowTitle)) {
            return false;
        }
        lastWindowTitle = newTitle;
        return true;
    }

    public static boolean pausedBasedOnLastTitle() {
        if (lastWindowTitle == null) {
            return false;
        }
        return lastWindowTitle.equals("Spotify Premium");
    }

    public static boolean initialized() {
        return spotifyWindow != null;
    }

    public static HWND getSpotifyHWND() {
        return spotifyWindow;
    }

    public static boolean isSpotifyInForeground() {
        HWND foregroundWindow = User32.INSTANCE.GetForegroundWindow();
        return foregroundWindow != null && foregroundWindow.equals(spotifyWindow);
    }
}

