import org.example.*;
import org.example.worker.ShuffleAlgorithm;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class MockedSecondaryMonitor {
    public static void main(String[] args) {
        System.out.println("This is a mocked secondary monitor for testing purposes.");

        PlayerStore playerStore = new PlayerStore();
        SecondaryMonitorGui secondaryMonitorGui = new SecondaryMonitorGui(playerStore, new PreferencesStore(), new ShuffleAlgorithm(new PlaylistStore(), new PlayerService(new PlayerStore(), new PlaybackDevicesStore())));
        secondaryMonitorGui.launchSecondaryMonitorGui(true);
//        secondaryMonitorGui.setProgress(1000000, 2000000);

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
