package org.example.util;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

public class ImageUtils {

    /**
     * Dims an image by multiplying its brightness.
     *
     * @param src       The source image to dim.
     * @param dimFactor Dim factor (0.0 = black, 1.0 = original brightness).
     * @return A new dimmed BufferedImage.
     */
    public static BufferedImage dimImage(BufferedImage src, float dimFactor) {
        if (dimFactor < 0f || dimFactor > 1f) {
            throw new IllegalArgumentException("dimFactor must be between 0.0 and 1.0");
        }

        BufferedImage dimmed = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = dimmed.createGraphics();

        // Draw the original image
        g2d.drawImage(src, 0, 0, null);

        // Overlay a black rectangle with dimFactor transparency
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - dimFactor));
        g2d.setColor(Color.black);
        g2d.fillRect(0, 0, src.getWidth(), src.getHeight());

        g2d.dispose();
        return dimmed;
    }

    /**
     * Applies a Gaussian blur to an image with edge extension.
     *
     * @param src        The source image to blur.
     * @param blurRadius The blur radius.
     * @return A new blurred BufferedImage.
     */
    public static BufferedImage blurWithEdgeExtension(BufferedImage src, int blurRadius) {
        if (blurRadius < 1) return src;

        int pad = blurRadius * 2;
        int w = src.getWidth();
        int h = src.getHeight();

        BufferedImage padded = new BufferedImage(w + pad * 2, h + pad * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = padded.createGraphics();

        // Draw center image
        g.drawImage(src, pad, pad, null);

        // Extend left/right edges
        for (int y = 0; y < h; y++) {
            int leftPixel = src.getRGB(0, y);
            int rightPixel = src.getRGB(w - 1, y);
            for (int x = 0; x < pad; x++) {
                padded.setRGB(x, y + pad, leftPixel);                     // Left side
                padded.setRGB(w + pad + x, y + pad, rightPixel);         // Right side
            }
        }

        // Extend top/bottom edges (including newly padded sides)
        for (int x = 0; x < padded.getWidth(); x++) {
            int topPixel = padded.getRGB(x, pad);
            int bottomPixel = padded.getRGB(x, h + pad - 1);
            for (int y = 0; y < pad; y++) {
                padded.setRGB(x, y, topPixel);                           // Top
                padded.setRGB(x, h + pad + y, bottomPixel);              // Bottom
            }
        }

        g.dispose();

        // Create Gaussian kernel
        float[] kernel = createGaussianKernel(blurRadius);
        Kernel hKernel = new Kernel(kernel.length, 1, kernel);
        Kernel vKernel = new Kernel(1, kernel.length, kernel);

        // Blur horizontally
        BufferedImage temp = new BufferedImage(padded.getWidth(), padded.getHeight(), BufferedImage.TYPE_INT_ARGB);
        new ConvolveOp(hKernel, ConvolveOp.EDGE_NO_OP, null).filter(padded, temp);

        // Blur vertically
        BufferedImage blurred = new BufferedImage(padded.getWidth(), padded.getHeight(), BufferedImage.TYPE_INT_ARGB);
        new ConvolveOp(vKernel, ConvolveOp.EDGE_NO_OP, null).filter(temp, blurred);

        // Crop back to original size
        return blurred.getSubimage(pad, pad, w, h);
    }

    private static float[] createGaussianKernel(int radius) {
        int size = radius * 2 + 1;
        float[] kernel = new float[size];
        float sigma = radius / 3.0f;  // standard deviation
        float sum = 0f;

        for (int i = 0; i < size; i++) {
            float x = i - (float) radius;
            kernel[i] = (float) Math.exp(-(x * x) / (2 * sigma * sigma));
            sum += kernel[i];
        }

        // Normalize
        for (int i = 0; i < size; i++) {
            kernel[i] /= sum;
        }

        return kernel;
    }

    /**
     * Creates a rounded corner image.
     *
     * @param image        The source image.
     * @param cornerRadius The radius of the corners.
     * @return A new BufferedImage with rounded corners.
     */
    public static BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new RoundRectangle2D.Float(0, 0, width, height, cornerRadius, cornerRadius));

        // Draw the original image within the rounded mask
        g2.drawImage(image, 0, 0, null);
        g2.dispose();

        return output;
    }
}
