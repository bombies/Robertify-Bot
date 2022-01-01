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

    public ImageBuilder addText(String text, Color textColor) {
        graphics.setColor(textColor);
        graphics.drawString(text, width/2, height/2);
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
