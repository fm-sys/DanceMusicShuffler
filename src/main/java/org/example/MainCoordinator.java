package org.example;

import org.example.api.LocalSpotifyProvider;
import org.example.models.PlaylistModel;
import org.example.worker.ShuffleAlgorithm;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;

import java.util.Collection;

public class MainCoordinator {
    private final QueuePresenter queuePresenter;
    private final NowPlayingPresenter nowPlayingPresenter;

    // todo: they are currently referenced from MainGui, ideally they can be private after refactoring is complete
    SecondaryMonitorGui secondaryMonitorGui = new SecondaryMonitorGui();

    PlaylistStore playlistStore = new PlaylistStore();
    PlayerStore playerStore = new PlayerStore();
    PlaybackDevicesStore playbackDevicesStore = new PlaybackDevicesStore();

    PlayerService playerService = new PlayerService(playerStore, playbackDevicesStore);

    ShuffleAlgorithm shuffleAlgorithm = new ShuffleAlgorithm(playlistStore);

    public MainCoordinator(Collection<PlaylistSimplified> lists) {

        // create presenters
        queuePresenter = new QueuePresenter(playerStore, shuffleAlgorithm);
        nowPlayingPresenter = new NowPlayingPresenter(playerService, shuffleAlgorithm);

        // create GUI
        MainGui mainGui = new MainGui(queuePresenter, nowPlayingPresenter, this);

        // init presenters
        queuePresenter.init(mainGui);
        nowPlayingPresenter.init(mainGui);

        LocalSpotifyProvider.INSTANCE.initialize(playerStore);

        // init data
        playlistStore.addPlaylists(lists.stream().map(PlaylistModel::new).toList());
        playerService.refreshAvailablePlaybackDevices();
        // playerService.refreshPlayerState();

    }











}
