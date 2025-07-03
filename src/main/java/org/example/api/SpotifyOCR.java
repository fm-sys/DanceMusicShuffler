package org.example.api;

import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinGDI.*;
import net.sourceforge.tess4j.*;
import net.sourceforge.tess4j.util.LoadLibs;
import org.example.OverlayWindow;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class SpotifyOCR {

    public static void main(String[] args) throws Exception {

        // Initialize Tesseract
        Tesseract tesseract = new Tesseract();
        File tessDataFolder = LoadLibs.extractTessResources("tessdata"); // Built-in tessdata
        tesseract.setDatapath(tessDataFolder.getAbsolutePath());
        tesseract.setLanguage("eng");


        // Example: HWND from elsewhere
        SpotifyWindowTitle.searchSpotifyWindowInitial(null);
        HWND hwnd = SpotifyWindowTitle.getSpotifyHWND();

        if (hwnd == null) {
            System.err.println("Spotify window not found.");
            return;
        }

        if (!SpotifyWindowTitle.isSpotifyInForeground()) {
            System.err.println("Spotify window is not focused.");
            return;
        }

        BufferedImage screenshot = captureWindow(hwnd);
        if (screenshot == null) {
            System.err.println("Failed to capture screenshot.");
            return;
        }

        // File imgFile = new File("spotify_capture.png");
        // ImageIO.write(screenshot, "png", imgFile);



        BufferedImage initialCrop = screenshot.getSubimage(0, 0, screenshot.getWidth(), 350);

        int xOffset = 0;
        int yOffset = 0;

        List<Word> initializingWords = tesseract.getWords(initialCrop, ITessAPI.TessPageIteratorLevel.RIL_TEXTLINE);

        for (Word word : initializingWords) {
            if (word.getText().contains("Warteschlange")) {
                xOffset = word.getBoundingBox().x + 50;
                yOffset = word.getBoundingBox().y + word.getBoundingBox().height;
            }
        }

        if (xOffset == 0) {
            System.err.println("Failed to find 'Warteschlange' in the initial crop.");
            return;
        }

        // OCR: Extract lines with bounding boxes
        BufferedImage cropped = screenshot.getSubimage(xOffset, yOffset, screenshot.getWidth() - xOffset, screenshot.getHeight() - yOffset);
        List<Word> lines = tesseract.getWords(cropped, ITessAPI.TessPageIteratorLevel.RIL_TEXTLINE);

        List<Rectangle> overlayBoxes = new java.util.ArrayList<>();
        for (Word line : lines) {
            Rectangle box = line.getBoundingBox();
            overlayBoxes.add(new Rectangle(box.x + xOffset, box.y + yOffset, box.width, box.height));
//            System.out.println("Line: " + line.getText());
        }

        // Get Spotify window position
        WinDef.RECT rect = new WinDef.RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);
        int spotifyX = rect.left + (rect.right - rect.left - 420); // x-position of cropped region
        int spotifyY = rect.top;
        int spotifyWidth = 420;
        int spotifyHeight = rect.bottom - rect.top;

        // Show overlay
        SwingUtilities.invokeLater(() ->
                new OverlayWindow(overlayBoxes, spotifyX, spotifyY, spotifyWidth, spotifyHeight)
        );

    }

    // 📷 Captures a screenshot of the window with given HWND
    private static BufferedImage captureWindow(HWND hwnd) {
        WinDef.RECT rect = new WinDef.RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);
        int width = rect.right - rect.left;
        int height = rect.bottom - rect.top;

        try {
            Robot robot = new Robot();
            return robot.createScreenCapture(new Rectangle(rect.left + width - 420, rect.top, 420, height));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
