package org.example.worker;

import java.awt.*;
import java.time.Duration;

public class PreventSleep {

    private static final long MOUSE_MOVE_INTERVAL = Duration.ofSeconds(30).toMillis();
    private static Thread preventSleepThread;
    private static boolean running = false;

    /**
     * Starts a background thread that moves the mouse by 1 pixel every interval
     * to prevent screen saver or sleep mode.
     */
    public static void startPreventingSleepLoop() {
        if (running) return; // Avoid double-start
        running = true;

        preventSleepThread = new Thread(() -> {
            try {
                final Robot robot = new Robot();
                int pixel = 1;
                while (running) {
                    moveMouseOnePixel(robot, pixel);
                    pixel *= -1; // Alternate direction
                    Thread.sleep(MOUSE_MOVE_INTERVAL);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        preventSleepThread.setDaemon(true); // So it doesn't block app exit
        preventSleepThread.start();
    }

    /**
     * Stops the background sleep-prevention thread.
     */
    public static void stopPreventingSleepLoop() {
        running = false;
        if (preventSleepThread != null) {
            preventSleepThread.interrupt();
        }
    }

    private static void moveMouseOnePixel(Robot robot, int pixel) {
        final Point location = MouseInfo.getPointerInfo().getLocation();
        final int x = (int) location.getX() + pixel;
        final int y = (int) location.getY();
        robot.mouseMove(x, y);
    }
}
