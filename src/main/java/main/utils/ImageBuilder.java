package main.utils;

import lombok.SneakyThrows;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class ImageBuilder {
    private final BufferedImage img;
    private final Graphics2D graphics;
    private final int width;
    private final int height;

    private ImageBuilder(int width, int height) {
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        graphics = img.createGraphics();
        this.width = width;
        this.height = height;
    }

    public ImageBuilder setBackground(Color color) {
           graphics.setColor(color);
           graphics.fillRect(0, 0, width, height);
           return this;
    }

    public ImageBuilder setBackground(Image img) {
        graphics.drawImage(img, 0,0, null);
        return this;
    }

    public ImageBuilder addText(String text, Color textColor, Font font,
                                int x, int y) {
        graphics.setColor(textColor);
        graphics.setFont(font);
        graphics.drawString(text, x, y);
        return this;
    }

    @SneakyThrows
    public File build(String fileName) {
        graphics.dispose();
        final File file = new File(fileName);
        ImageIO.write(img, "png", file);
        return file;
    }

    public static ImageBuilder create(int width, int height) {
        return new ImageBuilder(width, height);
    }
}
