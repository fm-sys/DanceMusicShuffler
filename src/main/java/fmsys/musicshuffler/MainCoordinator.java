package fmsys.musicshuffler;

import fmsys.musicshuffler.platform.LocalSpotifyProvider;
import fmsys.musicshuffler.model.PlaylistModel;
import fmsys.musicshuffler.presenter.NowPlayingPresenter;
import fmsys.musicshuffler.presenter.OptionsPresenter;
import fmsys.musicshuffler.presenter.PlaylistsPresenter;
import fmsys.musicshuffler.presenter.QueuePresenter;
import fmsys.musicshuffler.service.PlayerService;
import fmsys.musicshuffler.store.*;
import fmsys.musicshuffler.ui.MainWindow;
import fmsys.musicshuffler.ui.OcrOverlayWindow;
import fmsys.musicshuffler.ui.PresentationWindow;
import fmsys.musicshuffler.service.ShuffleAlgorithm;
import fmsys.musicshuffler.platform.SpotifyOcrIntegration;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;

import java.util.Collection;

public class MainCoordinator {
    // todo: currently referenced from MainWindow, ideally this dependency should be resolved
    public PresentationWindow presentationWindow;

    public final PreferencesStore preferencesStore = new PreferencesStore();
    public final PlaylistStore playlistStore = new PlaylistStore();
    public final FilterStore filterStore = new FilterStore();
    public final PlayerStore playerStore = new PlayerStore();
    public final PlaybackDevicesStore playbackDevicesStore = new PlaybackDevicesStore();

    public final PlayerService playerService = new PlayerService(playerStore, playbackDevicesStore);

    public final ShuffleAlgorithm shuffleAlgorithm = new ShuffleAlgorithm(playlistStore, playerService);

    public final OcrOverlayWindow ocrOverlayWindow = new OcrOverlayWindow(playerStore, shuffleAlgorithm);
    public final SpotifyOcrIntegration spotifyOcrProcessor = SpotifyOcrIntegration.create(ocrOverlayWindow);


    public MainCoordinator(Collection<PlaylistSimplified> lists) {

        // create presenters
        QueuePresenter queuePresenter = new QueuePresenter(playerStore, shuffleAlgorithm);
        NowPlayingPresenter nowPlayingPresenter = new NowPlayingPresenter(playerService, shuffleAlgorithm);
        PlaylistsPresenter playlistsPresenter =  new PlaylistsPresenter(playlistStore, filterStore);
        OptionsPresenter optionsPresenter = new OptionsPresenter(playlistStore, preferencesStore, filterStore, shuffleAlgorithm);

        // create GUI
        presentationWindow = new PresentationWindow(playerStore, preferencesStore, shuffleAlgorithm);
        MainWindow mainWindow = new MainWindow(queuePresenter, nowPlayingPresenter, playlistsPresenter, optionsPresenter, this);

        // init presenters
        queuePresenter.init(mainWindow);
        nowPlayingPresenter.init(mainWindow);
        playlistsPresenter.init(mainWindow);
        optionsPresenter.init(mainWindow);

        LocalSpotifyProvider.INSTANCE.initialize(playerStore);

        // init data
        playlistStore.setState(lists.stream().map(PlaylistModel::new).toList());
        playerService.refreshAvailablePlaybackDevices();
        // playerService.refreshPlayerState();

    }

}
