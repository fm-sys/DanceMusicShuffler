package org.example.gui;

import javax.swing.*;
import java.awt.*;

public class TristateCheckBox extends JCheckBox {

    private boolean halfState;

    @Override
    public void paint(Graphics g) {
        if (isSelected()) {
            halfState = false;
        }
        super.paint(g);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (halfState) {
            // Get the checkbox bounds
            Insets insets = getInsets();
            int height = getHeight() - insets.top - insets.bottom;

            // Define checkbox dimensions
            int boxSize = height - 8;
            int x = insets.left + 3;
            int y = insets.top + 4;

            // Draw the indeterminate (half-selected) mark
            g.setColor(new Color(25, 110, 191));
            //g.fillRect(x + 3, y + boxSize / 2 - 2, boxSize - 6, 4); // Draws a horizontal bar
            g.fillRect(x, y, boxSize, boxSize);
        }
    }

    public boolean isHalfSelected() {
        return halfState;
    }

    public void setHalfSelected(boolean halfState) {
        this.halfState = halfState;
        if (halfState) {
            setSelected(false);
            repaint();
        }
    }
}