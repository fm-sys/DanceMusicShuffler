package org.example;

import org.example.gui.DotLoadingIndicator;

import javax.swing.*;
import java.awt.*;

public class SplashScreen {
    JWindow splash;

    public SplashScreen() {
        splash = new JWindow();
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));

        JLabel title = new JLabel("Dance Music Shuffler", JLabel.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        content.add(title, BorderLayout.NORTH);

        DotLoadingIndicator loadingIndicator = new DotLoadingIndicator();
        content.add(loadingIndicator, BorderLayout.CENTER);

        splash.getContentPane().add(content);
        splash.setSize(250, 150);
        splash.setLocationRelativeTo(null);
        splash.setVisible(true);
        splash.setAlwaysOnTop(true);
        splash.toFront();
    }

    public void dispose() {
        splash.setVisible(false);
        splash.dispose();
    }


}

