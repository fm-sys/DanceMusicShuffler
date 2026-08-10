package fmsys.musicshuffler.ui.main;

import com.formdev.flatlaf.FlatClientProperties;
import fmsys.musicshuffler.model.PlaylistModel;
import fmsys.musicshuffler.presenter.NowPlayingPresenter;
import fmsys.musicshuffler.presenter.OptionsPresenter;
import fmsys.musicshuffler.store.PlaylistStore;
import fmsys.musicshuffler.store.PreferenceParams;
import fmsys.musicshuffler.ui.PresentationWindow;
import fmsys.musicshuffler.ui.components.AlignHelper;
import fmsys.musicshuffler.view.OptionsView;

import javax.swing.*;
import java.awt.*;
import java.util.List;

@SuppressWarnings("UnnecessaryUnicodeEscape")
public class OptionsPanel extends JPanel implements OptionsView {

    JSpinner songNumberSpinner;
    JSpinner cooldownSpinner;
    JCheckBox groupPlaylistsCheckbox;
    JCheckBox presentationWindowShowSideSheetCheckbox;
    JCheckBox presentationWindowCoverCheckbox;
    JCheckBox presentationWindowColoredBackgroundCheckbox;
    JButton loadAndShuffleButton;
    JButton loadPrefsButton;
    private final JFrame frame;
    private final NowPlayingPresenter nowPlayingPresenter;
    private boolean updatingPreferencesFromStore;

    OptionsPanel(JFrame frame, OptionsPresenter optionsPresenter, PresentationWindow presentationWindow, PlaylistStore playlistStore, NowPlayingPresenter nowPlayingPresenter) {
        this.frame = frame;
        this.nowPlayingPresenter = nowPlayingPresenter;

        setBackground(MainWindow.PANEL_COLOR);
        putClientProperty(FlatClientProperties.STYLE, "arc: 32");
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(Box.createVerticalGlue());

        JPanel labeledPanelOptions = new JPanel();
        labeledPanelOptions.setBackground(MainWindow.PANEL_COLOR);
        labeledPanelOptions.setLayout(new BoxLayout(labeledPanelOptions, BoxLayout.Y_AXIS));
        labeledPanelOptions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Options"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        add(labeledPanelOptions);

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

        add(Box.createVerticalStrut(10));

        JPanel labeledPanelConfig = new JPanel(new GridLayout(0, 2, 10, 0));
        labeledPanelConfig.setBackground(MainWindow.PANEL_COLOR);
        labeledPanelConfig.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Load/Store Configuration"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        labeledPanelConfig.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
        add(labeledPanelConfig);

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

        add(Box.createVerticalStrut(10));

        JPanel labeledPanelMonitor = new JPanel(new GridLayout(0, 1));
        labeledPanelMonitor.setBackground(MainWindow.PANEL_COLOR);
        labeledPanelMonitor.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Secondary Monitor"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        labeledPanelMonitor.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
        add(labeledPanelMonitor);

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
            if (!presentationWindow.launch(false)) {
                int result = JOptionPane.showConfirmDialog(frame, "Secondary monitor not detected. Open Anyway?", "No second monitor", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    presentationWindow.launch(true);
                }
            }
        });
        labeledPanelMonitor.add(launchPresentationWindowButton);

        add(Box.createVerticalStrut(10));

        loadAndShuffleButton = new JButton("Load Playlists and Shuffle");
        loadAndShuffleButton.putClientProperty(FlatClientProperties.STYLE, "arc: 16");
        loadAndShuffleButton.addActionListener(e -> {
            if (playlistStore.getSelectedPlaylists().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please select at least one playlist to shuffle.");
                return;
            }
            loadAndShuffleButton.setEnabled(false);
            loadAndShuffleButton.setText("Loading...");
            optionsPresenter.onLoadAndShuffleClicked();
        });
        JPanel expandedButtonPanel = new JPanel(new GridLayout(0, 1));
        expandedButtonPanel.setBorder(BorderFactory.createEmptyBorder(0, 2, 2, 2));
        expandedButtonPanel.setBackground(MainWindow.PANEL_COLOR);
        expandedButtonPanel.setPreferredSize(new Dimension(0, 50));
        expandedButtonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
        expandedButtonPanel.add(loadAndShuffleButton);
        frame.getRootPane().setDefaultButton(loadAndShuffleButton);
        add(expandedButtonPanel);

        add(Box.createVerticalGlue());
    }

    JButton getLoadAndShuffleButton() {
        return loadAndShuffleButton;
    }

    @Override
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
    public void showExclusivePoolDialog(java.util.List<PlaylistModel> selectedPlaylists) {
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
}
