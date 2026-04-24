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

    public static Component pad(Component panel, Insets insets) {
        Box bh = Box.createHorizontalBox();
        bh.add(Box.createRigidArea(new Dimension(insets.left, 0)));

        Box bv = Box.createVerticalBox();
        bv.add(Box.createVerticalStrut(insets.top));
        bv.add(panel);
        bv.add(Box.createVerticalStrut(insets.bottom));
        bh.add(bv);

        bh.add(Box.createRigidArea(new Dimension(insets.right, 0)));
        return bh;

        /*
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(new Color(0, 0, 0, 0));
        wrapper.setBorder(BorderFactory.createEmptyBorder(
                insets.top, insets.left, insets.bottom, insets.right
        ));
        wrapper.add(panel, BorderLayout.CENTER);
        return wrapper;
         */
    }
}
