package org.example.gui;

import javax.swing.*;
import java.awt.*;

public class AlignHelper {

    private AlignHelper() {
        // Prevent instantiation
    }

    public static Component left(Component panel) {
        Box b = Box.createHorizontalBox();
        b.add(panel);
        b.add(Box.createHorizontalGlue());
        return b;
    }

    public static Component right(Component panel) {
        Box b = Box.createHorizontalBox();
        b.add(Box.createHorizontalGlue());
        b.add(panel);
        return b;
    }

    public static Component center(Component panel) {
        Box b = Box.createHorizontalBox();
        b.add(Box.createHorizontalGlue());
        b.add(panel);
        b.add(Box.createHorizontalGlue());
        return b;
    }
}
