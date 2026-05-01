package fmsys.musicshuffler.presenter;

import fmsys.musicshuffler.view.NowPlayingView;
import fmsys.musicshuffler.service.PlayerService;
import fmsys.musicshuffler.store.PlayerState;
import fmsys.musicshuffler.platform.LocalSpotifyProvider;
import fmsys.musicshuffler.model.PlaybackDevice;
import fmsys.musicshuffler.util.Scheduler;
import fmsys.musicshuffler.service.ShuffleAlgorithm;

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

        new Timer(1000, evt -> {
            if (LocalSpotifyProvider.INSTANCE.titleHasChanged()) {
                Scheduler.waitForWebApiDelayAndRun(this::triggerPlayerRefresh);
            } else if (playerService.playerStore.get().getProgressPercentage() == 1.0 && playerService.playerStore.get().isPlaying()) {
                // Song is about to end, refresh player state to get the next song
                Scheduler.waitForWebApiDelayAndRun(this::triggerPlayerRefresh);
            }
        }).start();
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
