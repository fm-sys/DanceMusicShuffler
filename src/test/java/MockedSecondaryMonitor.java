import org.example.SecondaryMonitorGui;
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

        SecondaryMonitorGui secondaryMonitorGui = new SecondaryMonitorGui();
        secondaryMonitorGui.launchSecondaryMonitorGui(true);
        secondaryMonitorGui.setProgress(1000000, 2000000);

        Track dummyTrack = new Track.Builder()
                .setName("Test Song")
                .setArtists(new ArtistSimplified.Builder().setName("Test Artist").build())
                .build();

        BufferedImage backgroundImage;
        try {
            backgroundImage = ImageIO.read(new URI("https://wallpaperaccess.com/full/1762891.jpg").toURL());
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }

        secondaryMonitorGui.setCoverVisible(false);
        secondaryMonitorGui.update(null, backgroundImage, dummyTrack, List.of("Badge A", "Badge B", "Badge C"));
        secondaryMonitorGui.updateSidePanel(List.of(List.of("First Badge"), List.of("Second Badge", "Another Badge"), List.of("Third Badge")));
    }
}
