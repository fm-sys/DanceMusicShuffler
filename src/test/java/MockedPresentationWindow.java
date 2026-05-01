import fmsys.musicshuffler.service.PlayerService;
import fmsys.musicshuffler.store.*;
import fmsys.musicshuffler.ui.PresentationWindow;
import fmsys.musicshuffler.service.ShuffleAlgorithm;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class MockedPresentationWindow {
    public static void main(String[] args) {
        System.out.println("This is a mocked dance floor display for testing purposes.");

        PlayerStore playerStore = new PlayerStore();
        PresentationWindow presentationWindow = new PresentationWindow(playerStore, new PreferencesStore(), new ShuffleAlgorithm(new PlaylistStore(), new PlayerService(new PlayerStore(), new PlaybackDevicesStore())));
        presentationWindow.launch(true);

        Track dummyTrack = new Track.Builder()
                .setName("Test Song")
                .setArtists(new ArtistSimplified.Builder().setName("Test Artist").build())
                .setDurationMs(3 * 60 * 1000)
                .build();

        BufferedImage backgroundImage;
        try {
            backgroundImage = ImageIO.read(new URI("https://wallpaperaccess.com/full/1762891.jpg").toURL());
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }

        playerStore.setState(new PlayerState(dummyTrack, null, backgroundImage, true, 60 * 1000, System.currentTimeMillis(), List.of()));
    }
}
