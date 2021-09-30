package main.utils;

import main.main.Robertify;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;

public class GeneralUtils {

    public static void setDefaultEmbed() {
        EmbedUtils.setEmbedBuilder(
                () -> new EmbedBuilder()
                        .setTitle("Robertify")
                        .setColor(new Color(51, 255, 0))
                        .setFooter(Robertify.api.getSelfUser().getAsTag())
        );
    }

    public static void setCustomEmbed(Color color) {
        EmbedUtils.setEmbedBuilder(
                () -> new EmbedBuilder()
                        .setColor(color)
                        .setFooter(Robertify.api.getSelfUser().getAsTag())
        );
    }

    public static void setCustomEmbed(String title, Color color) {
        EmbedUtils.setEmbedBuilder(
                () -> new EmbedBuilder()
                        .setColor(color)
                        .setTitle(title)
                        .setFooter(Robertify.api.getSelfUser().getAsTag())
        );
    }

    public static void setCustomEmbed(String title) {
        EmbedUtils.setEmbedBuilder(
                () -> new EmbedBuilder()
                        .setColor(new Color(247, 90, 90))
                        .setTitle(title)
                        .setFooter(Robertify.api.getSelfUser().getAsTag())
        );
    }

    public static void setCustomEmbed(String title, String footer) {
        EmbedUtils.setEmbedBuilder(
                () -> new EmbedBuilder()
                        .setColor(new Color(247, 90, 90))
                        .setTitle(title)
                        .setFooter(footer)
        );
    }

    public static void setCustomEmbed(String title, Color color, String footer) {
        EmbedUtils.setEmbedBuilder(
                () -> new EmbedBuilder()
                        .setColor(color)
                        .setTitle(title)
                        .setFooter(footer)
        );
    }
}
