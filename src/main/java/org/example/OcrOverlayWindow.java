package org.example;

import net.sourceforge.tess4j.Word;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.example.models.TrackWithBadges;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OcrOverlayWindow extends JWindow {
    private List<TrackWithBadges> queueTracks = new ArrayList<>();
    private final List<OverlayItem> overlayItems = new ArrayList<>();

    JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

    public OcrOverlayWindow() {
        super();
        setAlwaysOnTop(true);
        setBackground(new Color(0, 0, 0, 0));
        add(new OverlayPanel());
        setVisible(true);
    }

    public void clearOverlay() {
        overlayItems.clear();
        repaint();
    }

    public void updateBoxes(List<Word> lines, int x, int y, int width, int height) {
        setBounds(x - 560, y, 500, height);

        overlayItems.clear();

        int queuePosition = 0;
        int mayFit = -1;
        for (Word line : lines) {
            if (mayFit > -1) {
                String artists = Arrays.stream(((Track) queueTracks.get(mayFit).track()).getArtists())
                        .map(ArtistSimplified::getName)
                        .collect(Collectors.joining(", "));
                double similarityScore = Math.max(similarity.apply(artists, line.getText()), similarity.apply("Musikvideo - " + line.getText(), line.getText()));
//                System.out.println(similarityScore + "similarity for \"" + artists + "\" and \"" + line.getText() + "\"");
                if (similarityScore > 0.6) {
                    overlayItems.add(new OverlayItem(queueTracks.get(mayFit).badges().stream().findFirst().orElse("- kein badge -"), (int) line.getBoundingBox().getY()));
                    queuePosition = mayFit + 1;
                    mayFit = -1;
                    continue;
                }
                mayFit = -1;
            }

            for (int i = queuePosition; i < queueTracks.size(); i++) {
                double similarityScore = similarity.apply(queueTracks.get(i).track().getName(), line.getText());
//                System.out.println(similarityScore + "similarity for \"" + queueTracks.get(i).track().getName() + "\" and \"" + line.getText() + "\"");
                if (similarityScore > 0.6) {
                    mayFit = i;
                    break;
                }
            }
        }

        repaint();

    }

    public void updateQueue(List<TrackWithBadges> queueTracks) {
        this.queueTracks = queueTracks;
        repaint();
    }

    private record OverlayItem(String text, int ypos) {
    }

    private class OverlayPanel extends JPanel {
        public OverlayPanel() {
            setOpaque(false);
            setFont(new Font("SansSerif", Font.BOLD, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            for (OverlayItem item : overlayItems) {
                String text = item.text;
                int y = item.ypos + 2;

                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getHeight();

                int padding = 8;
                int arrowWidth = 10;

                int boxX = 500 - textWidth - 2 * padding - arrowWidth;
                int boxY = y - fm.getAscent() - padding;
                int boxWidth = textWidth + 2 * padding;
                int boxHeight = textHeight + 2 * padding;

                // Draw background box
                g2.setColor(Color.WHITE);
                g2.fillRect(boxX, boxY, boxWidth, boxHeight);

                // Draw right-pointing triangle beside the box (to the right)
                int arrowX = boxX + boxWidth;
                int arrowY = boxY + boxHeight / 2;

                Polygon arrow = new Polygon();
                arrow.addPoint(arrowX + arrowWidth, arrowY); // tip
                arrow.addPoint(arrowX, arrowY - boxHeight / 2); // top base
                arrow.addPoint(arrowX, arrowY + boxHeight / 2); // bottom base

                g2.fillPolygon(arrow);

                // Draw text
                g2.setColor(Color.BLACK);
                g2.drawString(text, boxX + padding, y);
            }


        }
    }
}
