package org.example.util;

import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public abstract class PopupMenuOpenedListener implements PopupMenuListener {
    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {}
}
