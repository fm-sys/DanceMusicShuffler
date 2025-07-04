package org.example.worker;

import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.HWND;
import net.sourceforge.tess4j.*;
import net.sourceforge.tess4j.util.LoadLibs;
import org.example.OcrOverlayWindow;
import org.example.api.SpotifyWindowTitle;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class SpotifyOcrProcessor implements Runnable {

    private volatile boolean running = false;
    private Thread thread;

    private final OcrOverlayWindow overlay;
    Tesseract tesseract;

    public SpotifyOcrProcessor(OcrOverlayWindow overlay) {
        this.overlay = overlay;

        tesseract = new Tesseract();
        File tessDataFolder = LoadLibs.extractTessResources("tessdata"); // Built-in tessdata
        tesseract.setDatapath(tessDataFolder.getAbsolutePath());
        tesseract.setLanguage("eng");
        tesseract.setVariable("user_defined_dpi", "300");
    }


    public void start() {
        if (running) return; // Already running
        running = true;
        thread = new Thread(this, "OCR-Processor");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        if (thread != null) thread.interrupt();
    }

    private static WinDef.RECT getWindowRect(HWND hwnd) {
        // Get Spotify window position
        WinDef.RECT rect = new WinDef.RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);
        return rect;
    }

    private static BufferedImage captureWindow(WinDef.RECT rect) {
        int height = rect.bottom - rect.top;

        try {
            Robot robot = new Robot();
            return robot.createScreenCapture(new Rectangle(rect.right - 420, rect.top, 420, height));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                if (!processSpotifyWindow()) {
                    // If processing fails, clear the overlay
                    SwingUtilities.invokeLater(overlay::clearOverlay);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                SwingUtilities.invokeLater(overlay::clearOverlay);
                break; // Exit if interrupted
            }
        }
    }

    private boolean processSpotifyWindow() {
        HWND hwnd = SpotifyWindowTitle.getSpotifyHWND();

        if (hwnd == null) {
            return false;
        }

        if (!SpotifyWindowTitle.isSpotifyInForeground()) {
            return false;
        }

        WinDef.RECT rect = getWindowRect(hwnd);

        BufferedImage screenshot = captureWindow(rect);
        if (screenshot == null) {
            return false;
        }

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
            return false;
        }

        // Check again in case the window lost focus, then it's not worth processing further
        if (!SpotifyWindowTitle.isSpotifyInForeground()) {
            return false;
        }

        // OCR: Extract lines with bounding boxes
        BufferedImage cropped = screenshot.getSubimage(xOffset, yOffset, screenshot.getWidth() - xOffset, screenshot.getHeight() - yOffset);
        List<Word> lines = tesseract.getWords(cropped, ITessAPI.TessPageIteratorLevel.RIL_TEXTLINE);

        // position of cropped region
        int overlayX = rect.right - 420 + xOffset;
        int overlayY = rect.top + yOffset;
        int overlayWidth = 420 - xOffset;
        int overlayHeight = rect.bottom - rect.top - yOffset;

        // Show overlay
        SwingUtilities.invokeLater(() ->
                overlay.updateBoxes(lines, overlayX, overlayY, overlayWidth, overlayHeight)
        );
        return true;
    }
}
