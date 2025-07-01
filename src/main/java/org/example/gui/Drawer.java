package org.example.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Drawer {

    private final JFrame frame;
    private final JPanel drawerPanel;
    private final JPanel glassPane;
    private Timer animator;

    private boolean isVisible = false;
    private int drawerWidth = 400;
    private float dimOpacity = 0f;

    public Drawer(JFrame frame) {
        this.frame = frame;

        // Drawer panel
        drawerPanel = new JPanel(new BorderLayout());
        drawerPanel.setBackground(Color.WHITE);

        // Glass pane for background dimming
        glassPane = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(new Color(0, 0, 0, (int)(dimOpacity * 255)));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        glassPane.setOpaque(false);
        glassPane.setVisible(false);

        // Add drawer to glass pane
        glassPane.add(drawerPanel);

        // Close on outside click
        glassPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getX() > drawerPanel.getWidth()) {
                    hideDrawer();
                }
            }
        });

        // Close on Esc
        setupEscapeKey();

        // Replace frame’s glass pane
        frame.setGlassPane(glassPane);

        // Animation
        animator = new Timer(5, e -> {
            int current = drawerPanel.getWidth();
            int target = isVisible ? drawerWidth : 0;
            int delta = (target - current) / 4;
            dimOpacity = Math.min(0.4f, Math.max(0f, (float) current / drawerWidth * 0.4f));
            if (delta == 0) {
                // Reached final target
                drawerPanel.setBounds(0, 0, target, frame.getHeight());
                dimOpacity = isVisible ? 0.4f : 0f;
                if (target == 0) {
                    glassPane.setVisible(false);
                }
                animator.stop();
            } else {
                int newWidth = current + delta;
                drawerPanel.setBounds(0, 0, newWidth, frame.getHeight());
            }

            glassPane.revalidate();
            glassPane.repaint();
        });

    }

    private void setupEscapeKey() {
        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        glassPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "closeDrawer");
        glassPane.getActionMap().put("closeDrawer", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hideDrawer();
            }
        });
    }

    public void setContent(Component component) {
        drawerPanel.removeAll();
        if (component != null) {
            drawerPanel.add(component, BorderLayout.CENTER);
        }
        drawerPanel.revalidate();
        drawerPanel.repaint();
    }

    public void setDrawerWidth(int width) {
        this.drawerWidth = width;
    }

    public void build() {
        drawerPanel.setBounds(0, 0, 0, frame.getHeight());
        glassPane.setVisible(false);
    }

    public void showDrawer() {
        if (!isVisible) {
            isVisible = true;
            glassPane.setVisible(true);
            animator.start();
        }
    }

    public void hideDrawer() {
        if (isVisible) {
            isVisible = false;
            if (!animator.isRunning()) {
                animator.start();
            }
        }
    }

    public void toggle() {
        if (isVisible) {
            hideDrawer();
        } else {
            showDrawer();
        }
    }

    public boolean isVisible() {
        return isVisible;
    }

    // Example usage
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Drawer Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setLayout(null);

            JButton openDrawerBtn = new JButton("Open Drawer");
            openDrawerBtn.setBounds(50, 50, 150, 30);
            frame.add(openDrawerBtn);

            Drawer drawer = new Drawer(frame);
            drawer.setContent(new JLabel("Hello from drawer!", SwingConstants.CENTER));
            drawer.setDrawerWidth(250);

            frame.setVisible(true); // Must be visible before build
            drawer.build();

            openDrawerBtn.addActionListener(e -> drawer.toggle());
        });
    }
}
