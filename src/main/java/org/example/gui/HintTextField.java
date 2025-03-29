package org.example.gui;

import javax.swing.*;
import java.awt.*;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class HintTextField extends JTextField {
    private boolean showingHint;

    public HintTextField(String hint) {
        this.showingHint = true;
        setForeground(Color.GRAY);
        setText(hint);

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (showingHint) {
                    setText("");
                    setForeground(Color.BLACK);
                    showingHint = false;
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (getText().isEmpty()) {
                    showingHint = true;
                    setText(hint);
                    setForeground(Color.GRAY);
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (showingHint) {
                    setText("");
                    setForeground(Color.BLACK);
                    showingHint = false;
                }
            }
        });
    }

    public String getTextWithoutHint() {
        return showingHint ? "" : super.getText();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Hint JTextField");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(300, 200);
            frame.setLayout(new FlowLayout());

            HintTextField textField = new HintTextField("Enter your name...");
            textField.setColumns(20);

            JButton button = new JButton("Get Text");
            button.addActionListener(e -> {
                System.out.println("Input: " + textField.getTextWithoutHint());
            });

            frame.add(textField);
            frame.add(button);
            frame.setVisible(true);
        });
    }
}

