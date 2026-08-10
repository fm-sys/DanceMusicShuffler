package fmsys.musicshuffler.ui.main;

import fmsys.musicshuffler.model.PlaylistModel;
import fmsys.musicshuffler.platform.LocalSpotifyProvider;
import fmsys.musicshuffler.platform.SpotifyOcrIntegration;
import fmsys.musicshuffler.presenter.NowPlayingPresenter;
import fmsys.musicshuffler.presenter.OptionsPresenter;
import fmsys.musicshuffler.presenter.PlaylistsPresenter;
import fmsys.musicshuffler.presenter.QueuePresenter;
import fmsys.musicshuffler.service.PlayerService;
import fmsys.musicshuffler.service.ShuffleAlgorithm;
import fmsys.musicshuffler.store.*;
import fmsys.musicshuffler.ui.OcrOverlayWindow;
import fmsys.musicshuffler.ui.PresentationWindow;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.Collection;

public class MainWindow extends JFrame {

    public static final Color PANEL_COLOR = new Color(25, 26, 28);

    private final PreferencesStore preferencesStore = new PreferencesStore();
    private final PlaylistStore playlistStore = new PlaylistStore();
    private final FilterStore filterStore = new FilterStore();
    private final PlayerStore playerStore = new PlayerStore();
    private final PlaybackDevicesStore playbackDevicesStore = new PlaybackDevicesStore();
    private final PlayerService playerService = new PlayerService(playerStore, playbackDevicesStore);
    private final ShuffleAlgorithm shuffleAlgorithm = new ShuffleAlgorithm(playlistStore, playerService);

    private final PresentationWindow presentationWindow = new PresentationWindow(playerStore, preferencesStore, shuffleAlgorithm);
    private final SpotifyOcrIntegration spotifyOcrProcessor = SpotifyOcrIntegration.create(new OcrOverlayWindow(playerStore, shuffleAlgorithm));
    private final OptionsPresenter optionsPresenter = new OptionsPresenter(playlistStore, preferencesStore, filterStore, shuffleAlgorithm);
    private final NowPlayingPresenter nowPlayingPresenter = new NowPlayingPresenter(playerService, shuffleAlgorithm);
    private final PlaylistsPresenter playlistsPresenter = new PlaylistsPresenter(playlistStore, filterStore);
    private final QueuePresenter queuePresenter = new QueuePresenter(playerStore, shuffleAlgorithm);

    private final OptionsPanel optionsPanel = new OptionsPanel(this, optionsPresenter, presentationWindow, playlistStore, nowPlayingPresenter);
    private final QueuePanel queuePanel = new QueuePanel(spotifyOcrProcessor);
    private final PlaylistsPanel playlistsPanel = new PlaylistsPanel(playlistsPresenter);
    private final NowPlayingPanel nowPlayingPanel = new NowPlayingPanel(nowPlayingPresenter);

    JSplitPane mainSplitPane;
    JSplitPane sideSplitPane;

    public MainWindow(Collection<PlaylistSimplified> lists) {
        setLayout(new BorderLayout());
        createMainContentPanels();

        LocalSpotifyProvider.INSTANCE.initialize(playerStore);
        playlistStore.setState(lists.stream().map(PlaylistModel::new).toList());
        playerService.refreshAvailablePlaybackDevices();

        addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                nowPlayingPresenter.triggerPlayerRefresh();
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                // not interested
            }
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Dance Music Shuffler");
        var icon = getClass().getResource("/icon48.png");
        if (icon != null) {
            setIconImage(new ImageIcon(icon).getImage());
        }
        setMinimumSize(new Dimension(600, 675));
        setSize(new Dimension(1000, 675));
        setLocationByPlatform(true);
        setVisible(true);
        SwingUtilities.invokeLater(() -> {
            sideSplitPane.setDividerLocation(300);
            SwingUtilities.invokeLater(() -> {
                mainSplitPane.setDividerLocation(400 - 4 * 8);
                toFront();
                optionsPanel.getLoadAndShuffleButton().requestFocusInWindow();
            });
        });
    }

    private void createMainContentPanels() {
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, optionsPanel, queuePanel);
        mainSplitPane.setResizeWeight(1.0);
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.putClientProperty("JSplitPane.expandableSide", "left");
        mainSplitPane.setBorder(BorderFactory.createEmptyBorder());

        sideSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, playlistsPanel, mainSplitPane);
        sideSplitPane.setResizeWeight(0);
        sideSplitPane.setOneTouchExpandable(true);
        sideSplitPane.putClientProperty("JSplitPane.expandableSide", "right");
        sideSplitPane.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

        add(sideSplitPane, BorderLayout.CENTER);
        add(nowPlayingPanel, BorderLayout.PAGE_END);

        queuePresenter.init(queuePanel);
        nowPlayingPresenter.init(nowPlayingPanel);
        playlistsPresenter.init(playlistsPanel);
        optionsPresenter.init(optionsPanel);
    }
}
