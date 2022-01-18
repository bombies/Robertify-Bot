package main.constants;

import net.dv8tion.jda.api.entities.Emoji;

import java.awt.*;

public enum RobertifyTheme {
    GREEN("https://i.imgur.com/ZFsTohQ.png", "https://i.imgur.com/oqVpzpV.png", "https://i.imgur.com/7vZpP2B.png", Emoji.fromMarkdown("<:robertifygreen2:933027476555309056>"), Color.decode("#2ce629")),
    RED("https://i.imgur.com/kPEgTan.png", "https://i.imgur.com/krfEYJP.png", "https://i.imgur.com/XRRcWJr.png", Emoji.fromMarkdown("<:robertifyred2:933027476660170783>"), Color.decode("#e62929")),
    GOLD("https://i.imgur.com/W6DLRII.png", "https://i.imgur.com/tPfRDTN.png", "https://i.imgur.com/wdOp9qS.png", Emoji.fromMarkdown("<:robertifygold2:933027476739854366>"), Color.decode("#ffac38")),
    PINK("https://i.imgur.com/sWia6Qj.png", "https://i.imgur.com/jirfaxT.png", "https://i.imgur.com/hkr4gDW.png", Emoji.fromMarkdown("<:robertifypink2:933027476639211570>"), Color.decode("#f159ff")),
    PURPLE("https://i.imgur.com/lHsH4qL.png", "https://i.imgur.com/XZ0dsEg.png", "https://i.imgur.com/09Vgk7E.png", Emoji.fromMarkdown("<:robertifypurple2:933027476534341722>"), Color.decode("#8900de")),
    BLUE("https://i.imgur.com/PkFNv8I.png", "https://i.imgur.com/EMPuJOU.png", "https://i.imgur.com/stJW754.png", Emoji.fromMarkdown("<:robertifyblue2:933027476752441374>"), Color.decode("#2b59ff")),
    LIGHT_BLUE("https://i.imgur.com/aS3RAfF.png", "https://i.imgur.com/IAN6WSf.png", "https://i.imgur.com/gsCGnY4.png", Emoji.fromMarkdown("<:robertifylightblue2:933027476748259328>"), Color.decode("#2bd8ff")),
    ORANGE("https://i.imgur.com/huYU6mh.png", "https://i.imgur.com/klJLrBn.png", "https://i.imgur.com/rWt38F3.png", Emoji.fromMarkdown("<:robertifyorange2:933027476672765982>"), Color.decode("#f25500")),
    YELLOW("https://i.imgur.com/VSaar3x.png", "https://i.imgur.com/rGYC1TZ.png", "https://i.imgur.com/ZJVB0XJ.png", Emoji.fromMarkdown("<:robertifyyellow2:933027476723093514>"), Color.decode("#ffea2b"));

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
