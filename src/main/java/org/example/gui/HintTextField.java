package org.example.gui;

import javax.swing.*;
import java.awt.*;

import java.awt.event.*;
import java.util.function.BooleanSupplier;

public class HintTextField extends JTextField {
    private boolean showingHint;
    private final String hint;

    public HintTextField(String hint) {
        this.showingHint = true;
        this.hint = hint;
        setForeground(Color.GRAY);
        super.setText(hint);

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (showingHint) {
                    HintTextField.super.setText("");
                    setForeground(Color.BLACK);
                    showingHint = false;
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (getText().isEmpty()) {
                    showingHint = true;
                    HintTextField.super.setText(hint);
                    setForeground(Color.GRAY);
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (showingHint) {
                    HintTextField.super.setText("");
                    setForeground(Color.BLACK);
                    showingHint = false;
                }
            }
        });

        suppressBeepForKey("BACK_SPACE", () -> getCaretPosition() > 0);
        suppressBeepForKey("DELETE", () -> getCaretPosition() < getText().length());
        suppressBeepForKey("ctrl BACK_SPACE", () -> getCaretPosition() > 0);
        suppressBeepForKey("ctrl DELETE", () -> getCaretPosition() < getText().length());
    }

    private void suppressBeepForKey(String keystrokeStr, BooleanSupplier shouldAct) {
        InputMap im = this.getInputMap();
        ActionMap am = this.getActionMap();

        KeyStroke keyStroke = KeyStroke.getKeyStroke(keystrokeStr);
        Object actionKey = im.get(keyStroke);
        Action originalAction = am.get(actionKey);

        if (actionKey != null && originalAction != null) {
            am.put(actionKey, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (shouldAct.getAsBoolean()) {
                        originalAction.actionPerformed(e);
                    }
                    // else do nothing (suppress beep)
                }
            });
        }
    }

    @Override
    public void setText(String text) {
        if (text == null || text.isEmpty()) {
            showingHint = true;
            super.setText(hint);
            setForeground(Color.GRAY);
        } else {
            showingHint = false;
            super.setText(text);
            setForeground(Color.BLACK);
        }
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

