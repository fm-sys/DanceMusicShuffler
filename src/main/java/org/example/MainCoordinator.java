package org.example;

import org.example.api.LocalSpotifyProvider;
import org.example.models.PlaylistModel;
import org.example.worker.ShuffleAlgorithm;
import org.example.worker.SpotifyOcrIntegration;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;

import java.util.Collection;

public class MainCoordinator {
    private final QueuePresenter queuePresenter;
    private final NowPlayingPresenter nowPlayingPresenter;
    private final PlaylistsPresenter playlistsPresenter;

    // todo: they are currently referenced from MainGui, ideally they can be private after refactoring is complete
    SecondaryMonitorGui secondaryMonitorGui;

    PreferencesStore preferencesStore = new PreferencesStore();
    PlaylistStore playlistStore = new PlaylistStore();
    FilterStore filterStore = new FilterStore();
    PlayerStore playerStore = new PlayerStore();
    PlaybackDevicesStore playbackDevicesStore = new PlaybackDevicesStore();

    PlayerService playerService = new PlayerService(playerStore, playbackDevicesStore);

    ShuffleAlgorithm shuffleAlgorithm = new ShuffleAlgorithm(playlistStore);

    final OcrOverlayWindow ocrOverlayWindow = new OcrOverlayWindow(playerStore, shuffleAlgorithm);
    final SpotifyOcrIntegration spotifyOcrProcessor = SpotifyOcrIntegration.create(ocrOverlayWindow);


    public MainCoordinator(Collection<PlaylistSimplified> lists) {

        // create presenters
        queuePresenter = new QueuePresenter(playerStore, shuffleAlgorithm);
        nowPlayingPresenter = new NowPlayingPresenter(playerService, shuffleAlgorithm);
        playlistsPresenter =  new PlaylistsPresenter(playlistStore, filterStore);

        // create GUI
        secondaryMonitorGui = new SecondaryMonitorGui(playerStore, preferencesStore, shuffleAlgorithm);
        MainGui mainGui = new MainGui(queuePresenter, nowPlayingPresenter, playlistsPresenter, this);

        // init presenters
        queuePresenter.init(mainGui);
        nowPlayingPresenter.init(mainGui);
        playlistsPresenter.init(mainGui);

        LocalSpotifyProvider.INSTANCE.initialize(playerStore);

        // init data
        playlistStore.setState(lists.stream().map(PlaylistModel::new).toList());
        playerService.refreshAvailablePlaybackDevices();
        // playerService.refreshPlayerState();

    }

}
