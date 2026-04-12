package org.example.util;

import javax.swing.*;

public class Scheduler {
    public static final int SPOTIFY_WEB_API_DELAY = 1000;

    private Scheduler() {
        // utility class
    }

    public static void waitForWebApiDelayAndRun(Runnable runnable) {
        Timer timer = new Timer(SPOTIFY_WEB_API_DELAY, e -> runnable.run());
        timer.setRepeats(false);
        timer.start();
    }
}
