package main.constants;

import net.dv8tion.jda.api.entities.Emoji;

import java.awt.*;

public enum RobertifyTheme {
    GREEN("https://i.imgur.com/Ct3A6Jv.png", "https://i.imgur.com/5TOxGsy.png", "https://i.imgur.com/nbuENrG.png", Emoji.fromMarkdown("<:robertify:929627982656598116>"), Color.decode("#2ce629")),
    RED("https://i.imgur.com/TRtUFtp.png", "https://i.imgur.com/Nz84NqQ.png", "https://i.imgur.com/kfBTk5Q.png", Emoji.fromMarkdown("<:robertifyred:929638640001376318>"), Color.decode("#e62929")),
    GOLD("https://i.imgur.com/2VTsHjX.png", "https://i.imgur.com/zTsYhFt.png", "https://i.imgur.com/uVHIMWF.png", Emoji.fromMarkdown("<:robertifygold:929627983298306049>"), Color.decode("#ffac38")),
    PINK("https://i.imgur.com/8yK0By9.png", "https://i.imgur.com/AHjvBhq.png", "https://i.imgur.com/LhWZj9d.png", Emoji.fromMarkdown("<:robertifypink:929638640391421972>"), Color.decode("#f159ff")),
    PURPLE("https://i.imgur.com/5MHnrDj.png", "https://i.imgur.com/5CknPoJ.png", "https://i.imgur.com/izE0wEw.png", Emoji.fromMarkdown("<:robertifypurple:929627984233644072>"), Color.decode("#8900de")),
    BLUE("https://i.imgur.com/4uj58ui.png", "https://i.imgur.com/PJ5VsN2.png", "https://i.imgur.com/RLUmQZW.png", Emoji.fromMarkdown("<:robertifyblue:929638640043315202>"), Color.decode("#2b59ff")),
    LIGHT_BLUE("https://i.imgur.com/8sHRtyz.png", "https://i.imgur.com/p7UwYE1.png", "https://i.imgur.com/CguiS5y.png", Emoji.fromMarkdown("<:robertifylightblue:929638640370470982>"), Color.decode("#2bd8ff")),
    ORANGE("https://i.imgur.com/DypLd48.png", "https://i.imgur.com/vHwEUMn.png", "https://i.imgur.com/7rpX8cT.png", Emoji.fromMarkdown("<:robertifyorange:929638639942631457>"), Color.decode("#f25500")),
    YELLOW("https://i.imgur.com/j98p7b2.png", "https://i.imgur.com/2JpiLgP.png", "https://i.imgur.com/ohAgso0.png", Emoji.fromMarkdown("<:robertifyyellow:929638640437575720>"), Color.decode("#ffea2b"));

    private final String transparent;
    private final String idleChannel;
    private final String playingMusic;
    private final Emoji emoji;
    private final Color color;

    RobertifyTheme(String transparent, String idleChannel, String playingMusic, Emoji emoji, Color color) {
        this.transparent = transparent;
        this.idleChannel = idleChannel;
        this.playingMusic = playingMusic;
        this.color = color;
        this.emoji = emoji;
    }


    public String getTransparent() {
        return this.transparent;
    }

    public String getIdleBanner() {
        return this.idleChannel;
    }

    public String getNowPlayingBanner() {
        return this.playingMusic;
    }

    public Emoji getEmoji() {
        return this.emoji;
    }

    public Color getColor() {
        return this.color;
    }

    public static RobertifyTheme parse(String str) {
        switch (str.toLowerCase()) {
            case "green" -> {
                return GREEN;
            }
            case "gold" -> {
                return GOLD;
            }
            case "red" -> {
                return RED;
            }
            case "pink" -> {
                return PINK;
            }
            case "purple" -> {
                return PURPLE;
            }
            case "blue" -> {
                return BLUE;
            }
            case "lightblue", "light_blue" -> {
                return LIGHT_BLUE;
            }
            case "orange" -> {
                return ORANGE;
            }
            case "yellow" -> {
                return YELLOW;
            }
            default -> throw new IllegalArgumentException("Invalid logo type!");
        }
    }
}
