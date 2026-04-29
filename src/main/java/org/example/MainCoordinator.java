package org.example;

import org.example.api.LocalSpotifyProvider;
import org.example.models.PlaylistModel;
import org.example.worker.ShuffleAlgorithm;
import org.example.worker.SpotifyOcrIntegration;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;

import java.util.Collection;

public class MainCoordinator {
    // todo: currently referenced from MainGui, ideally this dependency should be resolved
    SecondaryMonitorGui secondaryMonitorGui;

    PreferencesStore preferencesStore = new PreferencesStore();
    PlaylistStore playlistStore = new PlaylistStore();
    FilterStore filterStore = new FilterStore();
    PlayerStore playerStore = new PlayerStore();
    PlaybackDevicesStore playbackDevicesStore = new PlaybackDevicesStore();

    PlayerService playerService = new PlayerService(playerStore, playbackDevicesStore);

    ShuffleAlgorithm shuffleAlgorithm = new ShuffleAlgorithm(playlistStore, playerService);

    final OcrOverlayWindow ocrOverlayWindow = new OcrOverlayWindow(playerStore, shuffleAlgorithm);
    final SpotifyOcrIntegration spotifyOcrProcessor = SpotifyOcrIntegration.create(ocrOverlayWindow);


    public MainCoordinator(Collection<PlaylistSimplified> lists) {

        // create presenters
        QueuePresenter queuePresenter = new QueuePresenter(playerStore, shuffleAlgorithm);
        NowPlayingPresenter nowPlayingPresenter = new NowPlayingPresenter(playerService, shuffleAlgorithm);
        PlaylistsPresenter playlistsPresenter =  new PlaylistsPresenter(playlistStore, filterStore);
        OptionsPresenter optionsPresenter = new OptionsPresenter(playlistStore, preferencesStore, filterStore, shuffleAlgorithm);

        // create GUI
        secondaryMonitorGui = new SecondaryMonitorGui(playerStore, preferencesStore, shuffleAlgorithm);
        MainGui mainGui = new MainGui(queuePresenter, nowPlayingPresenter, playlistsPresenter, optionsPresenter, this);

        // init presenters
        queuePresenter.init(mainGui);
        nowPlayingPresenter.init(mainGui);
        playlistsPresenter.init(mainGui);
        optionsPresenter.init(mainGui);

        LocalSpotifyProvider.INSTANCE.initialize(playerStore);

        // init data
        playlistStore.setState(lists.stream().map(PlaylistModel::new).toList());
        playerService.refreshAvailablePlaybackDevices();
        // playerService.refreshPlayerState();

    }

}
