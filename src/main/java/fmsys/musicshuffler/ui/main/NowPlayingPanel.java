package fmsys.musicshuffler.ui.main;

import fmsys.musicshuffler.model.PlaybackDevice;
import fmsys.musicshuffler.presenter.NowPlayingPresenter;
import fmsys.musicshuffler.ui.components.BadgeLabel;
import fmsys.musicshuffler.view.NowPlayingView;
import se.michaelthelin.spotify.model_objects.interfaces.IArtist;
import se.michaelthelin.spotify.model_objects.specification.Track;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.stream.Collectors;

@SuppressWarnings("UnnecessaryUnicodeEscape")
public class NowPlayingPanel extends JPanel implements NowPlayingView {

    JPanel nowPlayingPanel;
    JComboBox<PlaybackDevice> devicesComboBox;
    private NowPlayingPresenter nowPlayingPresenter;

    private final ActionListener deviceChangeListener = (ActionEvent e) -> {
        PlaybackDevice selected = (PlaybackDevice) devicesComboBox.getSelectedItem();
        if (selected != null) {
            nowPlayingPresenter.onDeviceSelected(selected);
        }
    };

    public NowPlayingPanel(NowPlayingPresenter nowPlayingPresenter) {
        this.nowPlayingPresenter = nowPlayingPresenter;
        setLayout(new BorderLayout());

        nowPlayingPanel = new JPanel();
        nowPlayingPanel.setLayout(new BoxLayout(nowPlayingPanel, BoxLayout.X_AXIS));
        nowPlayingPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        nowPlayingPanel.setBackground(Color.black);


        JPanel buttonPanel = createPlaybackControlButtons();
        add(buttonPanel, BorderLayout.NORTH);
        add(nowPlayingPanel, BorderLayout.SOUTH);

        nowPlayingPresenter.triggerPlayerRefresh();
    }

    private JPanel createPlaybackControlButtons() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(Color.black);

        JButton restartButton = new JButton("\u275A\u25C0");
        restartButton.setOpaque(false);
        restartButton.setToolTipText("Restart the current song");
        restartButton.addActionListener(e -> nowPlayingPresenter.onRestartClicked());
        buttonPanel.add(restartButton);

        JButton playButton = new JButton("\u25B6\u275A\u275A");
        playButton.setOpaque(false);
        playButton.setToolTipText("Toggle play/pause");
        buttonPanel.add(playButton);
        playButton.addActionListener(e -> nowPlayingPresenter.onPlayPauseClicked());

        JButton forwardButton = new JButton("\u25B6\u25B6\u275A");
        forwardButton.setOpaque(false);
        forwardButton.setToolTipText("Skip to the next song");
        forwardButton.addActionListener(e -> nowPlayingPresenter.onSkipToNextClicked());
        buttonPanel.add(forwardButton);

        devicesComboBox = new JComboBox<>();
        devicesComboBox.setOpaque(false);
        devicesComboBox.setToolTipText("Choose playback device");
        devicesComboBox.setPrototypeDisplayValue(new PlaybackDevice("id", "A device name", false));
        devicesComboBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                nowPlayingPresenter.onDevicesComboBoxOpened();
            }
        });
        buttonPanel.add(devicesComboBox);

        return buttonPanel;
    }

    @Override
    public void showNowPlaying(Track track, java.util.List<String> badges) {
        nowPlayingPanel.removeAll();
        nowPlayingPanel.add(Box.createHorizontalGlue());
        nowPlayingPanel.add(Box.createRigidArea(new Dimension(0, BadgeLabel.defaultHeight())));
        nowPlayingPanel.add(new JLabel("Now playing: "));

        if (track != null) {
            String label = track.getName() +
                    " by " +
                    Arrays.stream(track.getArtists()).map(IArtist::getName).collect(Collectors.joining(", "));

            JLabel l = new JLabel(label);
            l.setFont(l.getFont().deriveFont(Font.BOLD));
            nowPlayingPanel.add(l);

            for (String badge : badges) {
                nowPlayingPanel.add(Box.createHorizontalStrut(5));
                nowPlayingPanel.add(new BadgeLabel(badge));
            }
        } else {
            nowPlayingPanel.add(new JLabel("- - -"));
        }

        nowPlayingPanel.add(Box.createHorizontalGlue());
        nowPlayingPanel.revalidate();
        nowPlayingPanel.repaint();
    }

    @Override
    public void updateDevicesComboBox(java.util.List<PlaybackDevice> devices) {
        devicesComboBox.removeActionListener(deviceChangeListener);

        devicesComboBox.removeAllItems();
        for (PlaybackDevice device : devices) {
            devicesComboBox.addItem(device);
            if (device.isActive()) {
                devicesComboBox.setSelectedItem(device);
            }
        }

        devicesComboBox.addActionListener(deviceChangeListener);
        devicesComboBox.setSelectedItem(devicesComboBox.getSelectedItem());

        if (devicesComboBox.isPopupVisible()) {
            // trigger re-rendering of popup
            devicesComboBox.hidePopup();
            devicesComboBox.showPopup();
        }
    }
}
