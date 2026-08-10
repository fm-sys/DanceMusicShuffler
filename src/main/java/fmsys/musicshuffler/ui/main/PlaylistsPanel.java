package fmsys.musicshuffler.ui.main;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.components.FlatTextField;
import com.formdev.flatlaf.extras.components.FlatTriStateCheckBox;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import com.formdev.flatlaf.ui.FlatEmptyBorder;
import fmsys.musicshuffler.model.PlaylistModel;
import fmsys.musicshuffler.presenter.PlaylistsPresenter;
import fmsys.musicshuffler.ui.components.AlignHelper;
import fmsys.musicshuffler.ui.components.TextChangedListener;
import fmsys.musicshuffler.view.PlaylistsView;

import javax.swing.*;
import java.awt.*;
import java.util.List;

@SuppressWarnings("UnnecessaryUnicodeEscape")
public class PlaylistsPanel extends JPanel implements PlaylistsView {
    FlatTriStateCheckBox selectAllCheckbox;
    FlatTextField playlistsFilterTextField;
    JPanel playlistsListPanel;
    private final PlaylistsPresenter playlistsPresenter;

    PlaylistsPanel(PlaylistsPresenter playlistsPresenter) {
        this.playlistsPresenter = playlistsPresenter;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(MainWindow.PANEL_COLOR);
        putClientProperty(FlatClientProperties.STYLE, "arc: 32");

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
        add(AlignHelper.pad(playlistsFilterTextField, new Insets(4, 4, 4, 4)));

        selectAllCheckbox = new FlatTriStateCheckBox();
        selectAllCheckbox.setAllowIndeterminate(false);
        selectAllCheckbox.addActionListener(e -> playlistsPresenter.selectAllCheckboxClicked(selectAllCheckbox.isSelected()));

        add(AlignHelper.pad(AlignHelper.left(selectAllCheckbox), new Insets(0, 8, 8, 8)));

        playlistsListPanel = new JPanel();
        playlistsListPanel.setLayout(new BoxLayout(playlistsListPanel, BoxLayout.Y_AXIS));
        playlistsListPanel.setBackground(MainWindow.PANEL_COLOR);

        JScrollPane scrollPane = new JScrollPane(playlistsListPanel);
        scrollPane.setBorder(new FlatEmptyBorder());
        scrollPane.getVerticalScrollBar().setBackground(MainWindow.PANEL_COLOR);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(AlignHelper.pad(scrollPane, new Insets(8, 8, 8, 0)));
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


}
