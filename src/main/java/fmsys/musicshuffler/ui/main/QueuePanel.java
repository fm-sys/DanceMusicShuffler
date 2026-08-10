package fmsys.musicshuffler.ui.main;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatEmptyBorder;
import fmsys.musicshuffler.model.TrackWithBadges;
import fmsys.musicshuffler.platform.SpotifyOcrIntegration;
import fmsys.musicshuffler.ui.components.AlignHelper;
import fmsys.musicshuffler.ui.components.BadgeLabel;
import fmsys.musicshuffler.view.QueueView;

import javax.swing.*;
import java.awt.*;

public class QueuePanel extends JPanel implements QueueView {
    JPanel queueListPanel;

    public QueuePanel(SpotifyOcrIntegration spotifyOcrProcessor) {
        setBackground(MainWindow.PANEL_COLOR);
        putClientProperty(FlatClientProperties.STYLE, "arc: 32");
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(Box.createVerticalStrut(5));

        JLabel queueLabel = new JLabel("Queue");
        queueLabel.setFont(queueLabel.getFont().deriveFont(Font.BOLD));
        add(AlignHelper.center(queueLabel));

        if (spotifyOcrProcessor.isSupported()) {
            JCheckBox ocrOverlayCheckbox = new JCheckBox("Enable Spotify Overlay (experimental)");
            ocrOverlayCheckbox.setSelected(false);
            ocrOverlayCheckbox.addActionListener(e -> {
                if (ocrOverlayCheckbox.isSelected()) {
                    spotifyOcrProcessor.start();
                } else {
                    spotifyOcrProcessor.stop();
                }
            });
            add(AlignHelper.center(ocrOverlayCheckbox));
        }

        add(Box.createVerticalStrut(5));

        queueListPanel = new JPanel();
        queueListPanel.setLayout(new BoxLayout(queueListPanel, BoxLayout.Y_AXIS));
        queueListPanel.setBackground(MainWindow.PANEL_COLOR);

        JScrollPane scrollPane = new JScrollPane(queueListPanel);
        scrollPane.setBorder(new FlatEmptyBorder());
        scrollPane.getVerticalScrollBar().setBackground(MainWindow.PANEL_COLOR);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(AlignHelper.pad(scrollPane, new Insets(8, 8, 8, 0)));
    }

    @Override
    public void showQueue(java.util.List<TrackWithBadges> queue) {
        queueListPanel.removeAll();
        int lineHeight = BadgeLabel.defaultHeight();

        queue.forEach(item -> {
            Box b = Box.createHorizontalBox();
            b.add(Box.createRigidArea(new Dimension(5, lineHeight)));
            JLabel label = new JLabel(item.track().getName());
            if (!item.fromShuffleAlgorithm()) {
                label.setForeground(Color.GRAY);
            }
            b.add(label);
            for (String badge : item.badges()) {
                b.add(Box.createHorizontalStrut(5));
                b.add(new BadgeLabel(badge));
            }
            b.add(Box.createHorizontalGlue());
            queueListPanel.add(b);
        });
        queueListPanel.revalidate(); // Updates layout
        queueListPanel.repaint();    // Redraws panel
    }
}
