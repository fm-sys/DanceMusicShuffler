package org.example;

import org.example.gui.BadgeLabel;
import org.example.gui.HintTextField;
import org.example.gui.TristateCheckBox;
import org.example.models.PlaylistModel;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class MainGui {

    ArrayList<PlaylistModel> playlists = new ArrayList<>();
    ArrayList<PlaylistModel> playlistsFiltered = new ArrayList<>();

    JLabel label;
    JFrame frame;
//    JPanel panel;

    String filterText;

    TristateCheckBox selectAllCheckbox;
    HintTextField playlistsFilterTextField;
    JPanel playlistsListPanel;

    public MainGui(Collection<PlaylistSimplified> lists) {

        lists.stream()
//                .sorted(Comparator.comparing(PlaylistSimplified::getName))
                .forEach(playlist -> playlists.add(new PlaylistModel(playlist)));
        playlistsFiltered.addAll(playlists);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }

        frame = new JFrame();
        frame.setLayout(new BorderLayout());

        label = new JLabel("Hello World");

//        panel = new JPanel();
//        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
//        //panel.setLayout(new GridLayout(0, 1));
//
//        panel.add(label, BorderLayout.CENTER);
//
        createPlaylistList();

        JPanel badgePanel = new JPanel();
        BadgeLabel label1 = new BadgeLabel("bla");
        label1.setText("This is a lable");
        badgePanel.add(label);

        frame.add(badgePanel, BorderLayout.LINE_END);

        frame.add(label, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Dance Music Shuffler");
        frame.setMinimumSize(new Dimension(300, 200));
        frame.setSize(new Dimension(800, 600));
        //frame.pack();
        frame.setVisible(true);
        frame.requestFocusInWindow();
    }

    private Component leftJustify(Component panel) {
        Box b = Box.createHorizontalBox();
        b.add(panel);
        b.add(Box.createHorizontalGlue());
        return b;
    }

    private void createPlaylistList() {

        JPanel outerPanel = new JPanel();
        outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));

        playlistsFilterTextField = new HintTextField("Filter playlists");
        playlistsFilterTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, playlistsFilterTextField.getPreferredSize().height));
        playlistsFilterTextField.addCaretListener(e -> {

            if (playlistsFilterTextField.getTextWithoutHint().equals(filterText)) {
                return; //Text hasn't changed
            }

            filterText = playlistsFilterTextField.getTextWithoutHint();
            if (filterText.isBlank()) {
                playlistsFiltered.clear();
                playlistsFiltered.addAll(playlists);
            } else {
                playlistsFiltered.clear();
                for (PlaylistModel playlist : playlists) {
                    if (playlist.getPlaylist().getName().toLowerCase().contains(filterText.toLowerCase().trim())) {
                        playlistsFiltered.add(playlist);
                    }
                }
            }
            recreatePlaylistsList();
        });
        outerPanel.add(playlistsFilterTextField);

        selectAllCheckbox = new TristateCheckBox();
        selectAllCheckbox.addActionListener(e -> {
            boolean isSelected = selectAllCheckbox.isSelected();
            playlistsFiltered.forEach(playlist -> playlist.setChecked(isSelected));
            recreatePlaylistsList();
        });


        outerPanel.add(leftJustify(selectAllCheckbox));

        playlistsListPanel = new JPanel();
        playlistsListPanel.setLayout(new BoxLayout(playlistsListPanel, BoxLayout.Y_AXIS));

        recreatePlaylistsList();

        JScrollPane scrollPane = new JScrollPane(playlistsListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        outerPanel.setPreferredSize(new Dimension(200, 0));

        outerPanel.add(scrollPane);

        frame.add(outerPanel, BorderLayout.LINE_START);

    }

    private void recreatePlaylistsList() {
        playlistsListPanel.removeAll();

        playlistsFiltered.forEach(playlist -> {
            JCheckBox checkBox = new JCheckBox(playlist.getPlaylist().getName() + " (" + playlist.getPlaylist().getTracks().getTotal() + " Lieder)");
            checkBox.setSelected(playlist.isChecked());
            checkBox.addActionListener(e -> {
                playlist.setChecked(checkBox.isSelected());
                updateSelectAllCheckbox();
            });
            playlistsListPanel.add(checkBox);
        });
        playlistsListPanel.revalidate(); // Updates layout
        playlistsListPanel.repaint();    // Redraws panel
        updateSelectAllCheckbox();
    }

    private void updateSelectAllCheckbox() {
        int selectedCount = (int) playlistsFiltered.stream().filter(PlaylistModel::isChecked).count();
        selectAllCheckbox.setText(selectedCount + " von " + playlistsFiltered.size() + " ausgewählt");
        if (selectedCount == 0) {
            selectAllCheckbox.setSelected(false);
            selectAllCheckbox.setHalfSelected(false);
        } else if (selectedCount != playlistsFiltered.size()) {
            selectAllCheckbox.setSelected(false);
            selectAllCheckbox.setHalfSelected(true);
        } else {
            selectAllCheckbox.setSelected(true);
            selectAllCheckbox.setHalfSelected(false);
            selectAllCheckbox.setText("Alle ausgewählt");
        }
    }

    // Layout types:
    //  BorderLayout (for overall Gui)
    //  BoxLayout (like Android linear layout)
    //  GroupLayout (mal sehen ob benötigt, kann zum Gruppieren von Elementen verwendet werden)
}
