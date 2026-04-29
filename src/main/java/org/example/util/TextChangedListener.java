package org.example.util;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public abstract class TextChangedListener implements DocumentListener {
    @Override
    public void insertUpdate(DocumentEvent e) {
        SwingUtilities.invokeLater(this::onChange);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        SwingUtilities.invokeLater(this::onChange);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        SwingUtilities.invokeLater(this::onChange);
    }

    public abstract void onChange();

}
