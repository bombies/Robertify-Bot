package main.constants;

import net.dv8tion.jda.api.entities.Emoji;

import java.awt.*;

public enum RobertifyTheme {
    GREEN("https://i.robertify.me/images/oczhg.png", "https://i.robertify.me/images/n27zq.jpg", "https://i.robertify.me/images/syy1c.jpg", Emoji.fromMarkdown("<:robertify:976955890542465044>"), Color.decode("#2ce629")),
    RED("https://i.robertify.me/images/unfss.png", "https://i.robertify.me/images/63ee9.jpg", "https://i.robertify.me/images/pexs2.jpg", Emoji.fromMarkdown("<:robertify_red:976955889850388540>"), Color.decode("#e62929")),
    GOLD("https://i.robertify.me/images/yn5av.png", "https://i.robertify.me/images/x9vcp.jpg", "https://i.robertify.me/images/pl6on.jpg", Emoji.fromMarkdown("<:robertify_gold:976955889481318470>"), Color.decode("#ffac38")),
    PINK("https://i.robertify.me/images/a35q9.png", "https://i.robertify.me/images/38azv.jpg", "https://i.robertify.me/images/fgq3m.jpg", Emoji.fromMarkdown("<:robertify_pink:976955889707794512>"), Color.decode("#f159ff")),
    PURPLE("https://i.robertify.me/images/dx0zs.png", "https://i.robertify.me/images/klbwb.jpg", "https://i.robertify.me/images/a2s47.jpg", Emoji.fromMarkdown("<:robertify_purple:976955889682616400>"), Color.decode("#8900de")),
    BLUE("https://i.robertify.me/images/5w9i8.png", "https://i.robertify.me/images/2tjlh.jpg", "https://i.robertify.me/images/v25jt.jpg", Emoji.fromMarkdown("<:robertify_blue:976955889342898296>"), Color.decode("#2b59ff")),
    LIGHT_BLUE("https://i.robertify.me/images/bbk4r.png", "https://i.robertify.me/images/98c80.jpg", "https://i.robertify.me/images/6tqvn.jpg", Emoji.fromMarkdown("<:robertify_light_blue:976955890068516924>"), Color.decode("#2bd8ff")),
    ORANGE("https://i.robertify.me/images/3svnd.png", "https://i.robertify.me/images/3lczl.jpg", "https://i.robertify.me/images/56fg5.jpg", Emoji.fromMarkdown("<:robertify_orange:976955889632305152>"), Color.decode("#f25500")),
    YELLOW("https://i.robertify.me/images/xmct0.png", "https://i.robertify.me/images/szkz9.jpg", "https://i.robertify.me/images/sly4k.jpg", Emoji.fromMarkdown("<:robertify_yellow:976955889829421126>"), Color.decode("#ffea2b")),
    DARK("https://i.robertify.me/images/4p6mu.png", "https://i.robertify.me/images/zu0il.jpg", "https://i.robertify.me/images/ql9ef.jpg", Emoji.fromMarkdown("<:robertify_dark:976955890001403954>"), Color.decode("#0f0f0f")),
    LIGHT("https://i.robertify.me/images/n57oz.png", "https://i.robertify.me/images/wr7td.jpg", "https://i.robertify.me/images/q3rwh.jpg", Emoji.fromMarkdown("<:robertify_light:976955889934290964>"), Color.decode("#f0f0f0"));

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
            case "dark" -> {
                return DARK;
            }
            case "light" -> {
                return LIGHT;
            }
            default -> throw new IllegalArgumentException("Invalid logo type!");
        }
    }
}
