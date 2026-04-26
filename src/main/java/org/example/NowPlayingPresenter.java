package org.example;

import org.example.models.PlaybackDevice;
import org.example.worker.ShuffleAlgorithm;

import javax.swing.*;
import java.util.List;

public class NowPlayingPresenter {

    private NowPlayingView view;

    private final PlayerService playerService;
    private final ShuffleAlgorithm shuffleAlgorithm;

    public NowPlayingPresenter(PlayerService playerService, ShuffleAlgorithm shuffleAlgorithm) {
        this.playerService = playerService;
        this.shuffleAlgorithm = shuffleAlgorithm;
    }

    public void init(NowPlayingView view) {
        this.view = view;
        playerService.playerStore.subscribe(this::update);
        playerService.playbackDevicesStore.subscribe(this::updatePlaybackDevices);

    }

    private void update(PlayerState state) {
        SwingUtilities.invokeLater(() -> view.showNowPlaying(state.track(), state.track() != null ? shuffleAlgorithm.getBadges(state.track()) : null));
    }

    private void updatePlaybackDevices(List<PlaybackDevice> playbackDevices) {
        SwingUtilities.invokeLater(() -> view.updateDevicesComboBox(playbackDevices));
    }

    public void onDeviceSelected(PlaybackDevice device) {
        playerService.transferPlaybackToDevice(device);
    }

    public void onDevicesComboBoxOpened() {
        playerService.refreshAvailablePlaybackDevices();
    }

    public void onPlayPauseClicked() {
        playerService.togglePlayback();
    }

    public void onSkipToNextClicked() {
        playerService.skipToNext();
    }

    public void onRestartClicked() {
        playerService.restartCurrent();
    }

    public void triggerPlayerRefresh() {
        playerService.refreshPlayerState();
    }
}
