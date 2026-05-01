package fmsys.musicshuffler.ui;

import fmsys.musicshuffler.MainCoordinator;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.components.FlatTextField;
import com.formdev.flatlaf.extras.components.FlatTriStateCheckBox;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import com.formdev.flatlaf.ui.FlatEmptyBorder;
import fmsys.musicshuffler.model.PlaybackDevice;
import fmsys.musicshuffler.model.PlaylistModel;
import fmsys.musicshuffler.model.TrackWithBadges;
import fmsys.musicshuffler.presenter.NowPlayingPresenter;
import fmsys.musicshuffler.presenter.OptionsPresenter;
import fmsys.musicshuffler.presenter.PlaylistsPresenter;
import fmsys.musicshuffler.presenter.QueuePresenter;
import fmsys.musicshuffler.store.PreferenceParams;
import fmsys.musicshuffler.ui.components.AlignHelper;
import fmsys.musicshuffler.ui.components.BadgeLabel;
import fmsys.musicshuffler.ui.components.TextChangedListener;
import fmsys.musicshuffler.view.NowPlayingView;
import fmsys.musicshuffler.view.OptionsView;
import fmsys.musicshuffler.view.PlaylistsView;
import fmsys.musicshuffler.view.QueueView;
import se.michaelthelin.spotify.model_objects.interfaces.IArtist;
import se.michaelthelin.spotify.model_objects.specification.Track;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("UnnecessaryUnicodeEscape")
public class MainWindow implements QueueView, NowPlayingView, PlaylistsView, OptionsView {

    public static final Color panelColor = new Color(25, 26, 28);

    final MainCoordinator coordinator; // todo: remove, handling should happen through presenters
    QueuePresenter queuePresenter;
    NowPlayingPresenter nowPlayingPresenter;
    PlaylistsPresenter playlistsPresenter;
    OptionsPresenter optionsPresenter;

    JFrame frame;

    FlatTriStateCheckBox selectAllCheckbox;
    FlatTextField playlistsFilterTextField;
    JPanel playlistsListPanel;

    JPanel queueListPanel;

    JPanel nowPlayingPanel;

    JSpinner songNumberSpinner;
    JSpinner cooldownSpinner;
    JCheckBox groupPlaylistsCheckbox;
    JCheckBox presentationWindowShowSideSheetCheckbox;
    JCheckBox presentationWindowCoverCheckbox;
    JCheckBox presentationWindowColoredBackgroundCheckbox;
    JButton loadAndShuffleButton;
    JButton loadPrefsButton;
    private boolean updatingPreferencesFromStore;

    JSplitPane mainSplitPane;
    JSplitPane sideSplitPane;

    JComboBox<PlaybackDevice> devicesComboBox;

    private final ActionListener deviceChangeListener = (ActionEvent e) -> {
        PlaybackDevice selected = (PlaybackDevice) devicesComboBox.getSelectedItem();
        if (selected != null) {
            nowPlayingPresenter.onDeviceSelected(selected);
        }
    };

    public MainWindow(QueuePresenter queuePresenter, NowPlayingPresenter nowPlayingPresenter, PlaylistsPresenter playlistsPresenter, OptionsPresenter optionsPresenter, MainCoordinator mainCoordinator) {
        this.queuePresenter = queuePresenter;
        this.nowPlayingPresenter = nowPlayingPresenter;
        this.playlistsPresenter = playlistsPresenter;
        this.optionsPresenter = optionsPresenter;
        this.coordinator = mainCoordinator;

        frame = new JFrame();
        frame.setLayout(new BorderLayout());
        createMainContentPanels();
        createNowPlaying();

        frame.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                nowPlayingPresenter.triggerPlayerRefresh();
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                // not interested
            }
        });

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Dance Music Shuffler");
        var icon = getClass().getResource("/icon48.png");
        if (icon != null) {
            frame.setIconImage(new ImageIcon(icon).getImage());
        }
        frame.setMinimumSize(new Dimension(600, 675));
        frame.setSize(new Dimension(1000, 675));
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
        SwingUtilities.invokeLater(() -> {
            sideSplitPane.setDividerLocation(300);
            SwingUtilities.invokeLater(() -> {
                mainSplitPane.setDividerLocation(400 - 4 * 8);
                frame.toFront();
                loadAndShuffleButton.requestFocusInWindow();
            });
        });
    }

    private void createMainContentPanels() {
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createCenterOptionsPanel(), createQueueList());
        mainSplitPane.setResizeWeight(1.0);
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.putClientProperty( "JSplitPane.expandableSide", "left" );
        mainSplitPane.setBorder(BorderFactory.createEmptyBorder());

        sideSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createPlaylistList(), mainSplitPane);
        sideSplitPane.setResizeWeight(0);
        sideSplitPane.setOneTouchExpandable(true);
        sideSplitPane.putClientProperty( "JSplitPane.expandableSide", "right" );
        sideSplitPane.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        frame.add(sideSplitPane, BorderLayout.CENTER);
    }

    private JPanel createCenterOptionsPanel() {
        JPanel centerOptionsPanel = new JPanel();
        centerOptionsPanel.setBackground(panelColor);
        centerOptionsPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 32");
        centerOptionsPanel.setLayout(new BoxLayout(centerOptionsPanel, BoxLayout.Y_AXIS));
        centerOptionsPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        centerOptionsPanel.add(Box.createVerticalGlue());

        JPanel labeledPanelOptions = new JPanel();
        labeledPanelOptions.setBackground(panelColor);
        labeledPanelOptions.setLayout(new BoxLayout(labeledPanelOptions, BoxLayout.Y_AXIS));
        labeledPanelOptions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Options"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        centerOptionsPanel.add(labeledPanelOptions);

        JLabel songNumberLabel = new JLabel("Number of songs to add to queue:");
        labeledPanelOptions.add(AlignHelper.left(songNumberLabel));

        songNumberSpinner = new JSpinner(new SpinnerNumberModel(10, 0, Integer.MAX_VALUE, 1));
        songNumberSpinner.setMaximumSize(new Dimension(songNumberSpinner.getPreferredSize().width, songNumberSpinner.getPreferredSize().height));
        songNumberSpinner.addChangeListener(e -> {
            if (!updatingPreferencesFromStore) {
                optionsPresenter.onCountChanged((int) songNumberSpinner.getValue());
            }
        });
        labeledPanelOptions.add(AlignHelper.pad(AlignHelper.left(songNumberSpinner), new Insets(4, 0, 4, 0)));

        labeledPanelOptions.add(Box.createVerticalStrut(10));

        JLabel cooldownLabel = new JLabel("Number of songs a playlist should not be reused:");
        labeledPanelOptions.add(AlignHelper.left(cooldownLabel));

        cooldownSpinner = new JSpinner(new SpinnerNumberModel(3, 0, Integer.MAX_VALUE, 1));
        cooldownSpinner.setMaximumSize(new Dimension(cooldownSpinner.getPreferredSize().width, cooldownSpinner.getPreferredSize().height));
        cooldownSpinner.addChangeListener(e -> {
            if (!updatingPreferencesFromStore) {
                optionsPresenter.onCooldownChanged((int) cooldownSpinner.getValue());
            }
        });
        labeledPanelOptions.add(AlignHelper.pad(AlignHelper.left(cooldownSpinner), new Insets(4, 0, 4, 0)));

        groupPlaylistsCheckbox = new JCheckBox("Group playlists based on names");
        groupPlaylistsCheckbox.setSelected(false);
        groupPlaylistsCheckbox.addActionListener(e -> {
            if (!updatingPreferencesFromStore) {
                optionsPresenter.onGroupPlaylistsChanged(groupPlaylistsCheckbox.isSelected());
            }
        });
        labeledPanelOptions.add(groupPlaylistsCheckbox);
        labeledPanelOptions.add(AlignHelper.left(groupPlaylistsCheckbox));

        labeledPanelOptions.add(Box.createVerticalStrut(10));

        JLabel exclusiveLabel = new JLabel("Playlists that may not be played directly after each other:");
        labeledPanelOptions.add(AlignHelper.left(exclusiveLabel));

        JButton exclusiveButton = new JButton("Select exclusive playlists");
        exclusiveButton.addActionListener(e -> optionsPresenter.onExclusiveClicked());
        labeledPanelOptions.add(AlignHelper.pad(AlignHelper.left(exclusiveButton), new Insets(4, 0, 4, 0)));

        labeledPanelOptions.add(Box.createVerticalStrut(10));

        JLabel weightsLabel = new JLabel("Configure how often a playlists is chosen:");
        labeledPanelOptions.add(AlignHelper.pad(AlignHelper.left(weightsLabel), new Insets(4, 0, 4, 0)));

        JButton weightsButton = new JButton("Adjust weights");
        weightsButton.addActionListener(e -> optionsPresenter.onWeightsClicked());
        labeledPanelOptions.add(AlignHelper.left(weightsButton));

        centerOptionsPanel.add(Box.createVerticalStrut(10));

        JPanel labeledPanelConfig = new JPanel(new GridLayout(0, 2, 10, 0));
        labeledPanelConfig.setBackground(panelColor);
        labeledPanelConfig.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Load/Store Configuration"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        labeledPanelConfig.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
        centerOptionsPanel.add(labeledPanelConfig);

        loadPrefsButton = new JButton("\u2191   Load configuration");
        loadPrefsButton.addActionListener(e -> {
            loadPrefsButton.setText("Loading...");
            loadPrefsButton.setEnabled(false);
            optionsPresenter.onLoadPrefsClicked();
        });
        labeledPanelConfig.add(loadPrefsButton);

        JButton storeButton = new JButton("\u2193   Store configuration");
        storeButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(frame, "This will overwrite any existing configuration. Do you want to continue?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
            optionsPresenter.onStorePrefsClicked();
        });
        labeledPanelConfig.add(storeButton);

        centerOptionsPanel.add(Box.createVerticalStrut(10));

        JPanel labeledPanelMonitor = new JPanel(new GridLayout(0, 1));
        labeledPanelMonitor.setBackground(panelColor);
        labeledPanelMonitor.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Secondary Monitor"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        labeledPanelMonitor.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
        centerOptionsPanel.add(labeledPanelMonitor);

        presentationWindowShowSideSheetCheckbox = new JCheckBox("Show side sheet");
        presentationWindowShowSideSheetCheckbox.setSelected(true);
        presentationWindowShowSideSheetCheckbox.addActionListener(e -> {
            if (!updatingPreferencesFromStore) {
                optionsPresenter.onShowSideSheetChanged(presentationWindowShowSideSheetCheckbox.isSelected());
            }
        });
        labeledPanelMonitor.add(presentationWindowShowSideSheetCheckbox);

        presentationWindowCoverCheckbox = new JCheckBox("Show cover");
        presentationWindowCoverCheckbox.setSelected(true);
        presentationWindowCoverCheckbox.addActionListener(e -> {
            if (!updatingPreferencesFromStore) {
                optionsPresenter.onCoverChanged(presentationWindowCoverCheckbox.isSelected());
            }
        });
        labeledPanelMonitor.add(presentationWindowCoverCheckbox);

        presentationWindowColoredBackgroundCheckbox = new JCheckBox("Use colored background");
        presentationWindowColoredBackgroundCheckbox.setSelected(true);
        presentationWindowColoredBackgroundCheckbox.addActionListener(e -> {
            if (!updatingPreferencesFromStore) {
                optionsPresenter.onColoredBackgroundChanged(presentationWindowColoredBackgroundCheckbox.isSelected());
            }
        });
        labeledPanelMonitor.add(presentationWindowColoredBackgroundCheckbox);

        JButton launchPresentationWindowButton = new JButton("Open Dance Floor Display");
        launchPresentationWindowButton.addActionListener(e -> {
            if (!coordinator.presentationWindow.launch(false)) {
                int result = JOptionPane.showConfirmDialog(frame, "Secondary monitor not detected. Open Anyway?", "No second monitor", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    coordinator.presentationWindow.launch(true);
                }
            }
        });
        labeledPanelMonitor.add(launchPresentationWindowButton);

        centerOptionsPanel.add(Box.createVerticalStrut(10));

        loadAndShuffleButton = new JButton("Load Playlists and Shuffle");
        loadAndShuffleButton.putClientProperty(FlatClientProperties.STYLE, "arc: 16");
        loadAndShuffleButton.addActionListener(e -> {
            if (coordinator.playlistStore.getSelectedPlaylists().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please select at least one playlist to shuffle.");
                return;
            }
            loadAndShuffleButton.setEnabled(false);
            loadAndShuffleButton.setText("Loading...");
            optionsPresenter.onLoadAndShuffleClicked();
        });
        JPanel expandedButtonPanel = new JPanel(new GridLayout(0, 1));
        expandedButtonPanel.setBorder(BorderFactory.createEmptyBorder(0, 2, 2, 2));
        expandedButtonPanel.setBackground(panelColor);
        expandedButtonPanel.setPreferredSize(new Dimension(0, 50));
        expandedButtonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
        expandedButtonPanel.add(loadAndShuffleButton);
        frame.getRootPane().setDefaultButton(loadAndShuffleButton);
        centerOptionsPanel.add(expandedButtonPanel);

        centerOptionsPanel.add(Box.createVerticalGlue());

        return centerOptionsPanel;
    }

    public void applyPreferences(PreferenceParams params) {
        updatingPreferencesFromStore = true;
        songNumberSpinner.setValue(params.count());
        cooldownSpinner.setValue(params.cooldown());
        groupPlaylistsCheckbox.setSelected(params.groupPlaylists());
        presentationWindowShowSideSheetCheckbox.setSelected(params.showSidePanel());
        presentationWindowCoverCheckbox.setSelected(params.showCover());
        presentationWindowColoredBackgroundCheckbox.setSelected(params.showBackground());
        loadPrefsButton.setText("\u2191   Load configuration");
        loadPrefsButton.setEnabled(true);
        updatingPreferencesFromStore = false;
    }

    @Override
    public void loadAndShuffleFinished(boolean success) {
        loadAndShuffleButton.setEnabled(true);
        loadAndShuffleButton.setText("Load Playlists and Shuffle");
        if (!success) {
            JOptionPane.showMessageDialog(frame, "Shuffle didn't complete", "Something went wrong", JOptionPane.ERROR_MESSAGE);
        }
        nowPlayingPresenter.triggerPlayerRefresh();
    }

    @Override
    public void showExclusivePoolDialog(List<PlaylistModel> selectedPlaylists) {
        if (selectedPlaylists.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please select playlists for the general pool first.");
            return;
        }

        JDialog dialog = new JDialog(frame, "Select exclusive playlists", true);
        dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(frame);

        JPanel checkboxesPanel = new JPanel();
        checkboxesPanel.setLayout(new BoxLayout(checkboxesPanel, BoxLayout.Y_AXIS));

        selectedPlaylists.forEach(playlist -> {
            JCheckBox checkBox = new JCheckBox(playlist.getPlaylist().getName());
            checkBox.setSelected(playlist.isExclusive());
            checkBox.addActionListener(event -> playlist.setExclusive(checkBox.isSelected()));
            checkboxesPanel.add(checkBox);
        });

        JScrollPane scrollPane = new JScrollPane(checkboxesPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        dialog.add(scrollPane);

        dialog.setVisible(true);
    }

    @Override
    public void showWeightsDialog(List<PlaylistModel> selectedPlaylists) {
        if (selectedPlaylists.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please select playlists for the general pool first.");
            return;
        }

        JDialog dialog = new JDialog(frame, "Adjust weights", true);
        dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(frame);

        JPanel weightsPanel = new JPanel();
        weightsPanel.setLayout(new BoxLayout(weightsPanel, BoxLayout.Y_AXIS));

        selectedPlaylists.forEach(playlist -> {
            JLabel label = new JLabel(playlist.getPlaylist().getName());
            JSpinner spinner = new JSpinner();
            spinner.setModel(new SpinnerNumberModel(playlist.getWeight(), 0, 10, 0.1));
            spinner.addChangeListener(e -> playlist.setWeight((double) spinner.getValue()));
            spinner.setPreferredSize(new Dimension(50, spinner.getPreferredSize().height));
            spinner.setMaximumSize(new Dimension(50, spinner.getPreferredSize().height));

            Box b = Box.createHorizontalBox();
            b.add(label);
            b.add(Box.createHorizontalGlue());
            b.add(spinner);

            weightsPanel.add(b);
            weightsPanel.add(Box.createVerticalStrut(5));
        });

        JScrollPane scrollPane = new JScrollPane(weightsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        dialog.add(scrollPane);

        dialog.setVisible(true);
    }

    private JPanel createQueueList() {

        JPanel outerPanel = new JPanel();
        outerPanel.setBackground(panelColor);
        outerPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 32");
        outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));
        outerPanel.add(Box.createVerticalStrut(5));

        JLabel queueLabel = new JLabel("Queue");
        queueLabel.setFont(queueLabel.getFont().deriveFont(Font.BOLD));
        outerPanel.add(AlignHelper.center(queueLabel));

        if (coordinator.spotifyOcrProcessor.isSupported()) {
            JCheckBox ocrOverlayCheckbox = new JCheckBox("Enable Spotify Overlay (experimental)");
            ocrOverlayCheckbox.setSelected(false);
            ocrOverlayCheckbox.addActionListener(e -> {
                if (ocrOverlayCheckbox.isSelected()) {
                    coordinator.spotifyOcrProcessor.start();
                } else {
                    coordinator.spotifyOcrProcessor.stop();
                }
            });
            outerPanel.add(AlignHelper.center(ocrOverlayCheckbox));
        }

        outerPanel.add(Box.createVerticalStrut(5));

        queueListPanel = new JPanel();
        queueListPanel.setLayout(new BoxLayout(queueListPanel, BoxLayout.Y_AXIS));
        queueListPanel.setBackground(panelColor);

        JScrollPane scrollPane = new JScrollPane(queueListPanel);
        scrollPane.setBorder(new FlatEmptyBorder());
        scrollPane.getVerticalScrollBar().setBackground(panelColor);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        outerPanel.add(AlignHelper.pad(scrollPane, new Insets(8, 8, 8, 0)));

        return outerPanel;
    }

    private int calculateBadgeHeight() {
        return new BadgeLabel("Dummy").getPreferredSize().height;
    }


    @Override
    public void showQueue(java.util.List<TrackWithBadges> queue) {
        queueListPanel.removeAll();
        int lineHeight = calculateBadgeHeight();

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

    private JPanel createPlaylistList() {

        JPanel outerPanel = new JPanel();
        outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));
        outerPanel.setBackground(panelColor);
        outerPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 32");

        playlistsFilterTextField = new FlatTextField();
        playlistsFilterTextField.setPlaceholderText("Filter playlists by title, description or owner...");
        playlistsFilterTextField.setLeadingIcon(new FlatSearchIcon());
        playlistsFilterTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, playlistsFilterTextField.getPreferredSize().height));
        playlistsFilterTextField.getDocument().addDocumentListener(new TextChangedListener() {
            @Override
            public void onChange() {
                playlistsPresenter.filterTextChanged(playlistsFilterTextField.getText());
            }
        });
        outerPanel.add(AlignHelper.pad(playlistsFilterTextField, new Insets(4, 4, 4, 4)));

        selectAllCheckbox = new FlatTriStateCheckBox();
        selectAllCheckbox.setAllowIndeterminate(false);
        selectAllCheckbox.addActionListener(e -> playlistsPresenter.selectAllCheckboxClicked(selectAllCheckbox.isSelected()));

        outerPanel.add(AlignHelper.pad(AlignHelper.left(selectAllCheckbox), new Insets(0, 8, 8, 8)));

        playlistsListPanel = new JPanel();
        playlistsListPanel.setLayout(new BoxLayout(playlistsListPanel, BoxLayout.Y_AXIS));
        playlistsListPanel.setBackground(panelColor);

        JScrollPane scrollPane = new JScrollPane(playlistsListPanel);
        scrollPane.setBorder(new FlatEmptyBorder());
        scrollPane.getVerticalScrollBar().setBackground(panelColor);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        outerPanel.add(AlignHelper.pad(scrollPane, new Insets(8, 8, 8, 0)));

        return outerPanel;
    }

    @Override
    public void setPlaylistsLists(List<PlaylistModel> playlists) {
        playlistsListPanel.removeAll();
        playlists.forEach(playlist -> {
            JCheckBox checkBox = new JCheckBox(playlist.getPlaylist().getName() + " (" + playlist.getPlaylist().getItems().getTotal() + " Lieder)" + (playlist.isFromConfig() ? " [playlist from config]" : ""));
            checkBox.setSelected(playlist.isChecked());
            checkBox.addActionListener(e -> playlistsPresenter.playlistCheckboxClicked(playlist, checkBox.isSelected()));
            playlistsListPanel.add(checkBox);
        });
        playlistsListPanel.revalidate(); // Updates layout
        playlistsListPanel.repaint();    // Redraws panel
    }

    @Override
    public void setFilterText(String filterText) {
        if (filterText.equals(playlistsFilterTextField.getText())) {
            return; // avoid infinite callback look
        }
        playlistsFilterTextField.setText(filterText);
    }

    @Override
    public void updateSelectAllCheckbox(int selected, int all) {
        selectAllCheckbox.setText(selected + " von " + all + " ausgew\u00e4hlt");
        if (selected == 0) {
            selectAllCheckbox.setState(FlatTriStateCheckBox.State.UNSELECTED);
        } else if (selected != all) {
            selectAllCheckbox.setState(FlatTriStateCheckBox.State.INDETERMINATE);
        } else {
            selectAllCheckbox.setState(FlatTriStateCheckBox.State.SELECTED);
            selectAllCheckbox.setText("Alle ausgew\u00e4hlt");
        }
    }

    private void createNowPlaying() {
        nowPlayingPanel = new JPanel();
        nowPlayingPanel.setLayout(new BoxLayout(nowPlayingPanel, BoxLayout.X_AXIS));
        nowPlayingPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        nowPlayingPanel.setBackground(Color.black);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());

        JPanel buttonPanel = createPlaybackControlButtons();
        bottomPanel.add(buttonPanel, BorderLayout.NORTH);
        bottomPanel.add(nowPlayingPanel, BorderLayout.SOUTH);

        frame.add(bottomPanel, BorderLayout.PAGE_END);
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

    @Override
    public void showNowPlaying(Track track, java.util.List<String> badges) {
        nowPlayingPanel.removeAll();
        nowPlayingPanel.add(Box.createHorizontalGlue());
        nowPlayingPanel.add(Box.createRigidArea(new Dimension(0, calculateBadgeHeight())));
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
}
