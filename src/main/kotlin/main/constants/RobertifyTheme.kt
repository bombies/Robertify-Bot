package main.constants

import net.dv8tion.jda.api.entities.emoji.Emoji
import java.awt.Color

enum class RobertifyTheme(
    val transparent: String,
    val idleBanner: String,
    val nowPlayingBanner: String,
    val emoji: Emoji,
    val color: Color
) {
    GREEN("https://i.robertify.me/images/oczhg.png", "https://i.robertify.me/images/n27zq.jpg", "https://i.robertify.me/images/syy1c.jpg", Emoji.fromFormatted("<:robertify:976955890542465044>"), Color.decode("#2ce629")),
    RED("https://i.robertify.me/images/unfss.png", "https://i.robertify.me/images/63ee9.jpg", "https://i.robertify.me/images/pexs2.jpg", Emoji.fromFormatted("<:robertify_red:976955889850388540>"), Color.decode("#e62929")),
    GOLD("https://i.robertify.me/images/yn5av.png", "https://i.robertify.me/images/x9vcp.jpg", "https://i.robertify.me/images/pl6on.jpg", Emoji.fromFormatted("<:robertify_gold:976955889481318470>"), Color.decode("#ffac38")),
    PINK("https://i.robertify.me/images/a35q9.png", "https://i.robertify.me/images/38azv.jpg", "https://i.robertify.me/images/fgq3m.jpg", Emoji.fromFormatted("<:robertify_pink:976955889707794512>"), Color.decode("#f159ff")),
    PURPLE("https://i.robertify.me/images/dx0zs.png", "https://i.robertify.me/images/klbwb.jpg", "https://i.robertify.me/images/a2s47.jpg", Emoji.fromFormatted("<:robertify_purple:976955889682616400>"), Color.decode("#8900de")),
    BLUE("https://i.robertify.me/images/5w9i8.png", "https://i.robertify.me/images/2tjlh.jpg", "https://i.robertify.me/images/v25jt.jpg", Emoji.fromFormatted("<:robertify_blue:976955889342898296>"), Color.decode("#2b59ff")),
    LIGHT_BLUE("https://i.robertify.me/images/bbk4r.png", "https://i.robertify.me/images/98c80.jpg", "https://i.robertify.me/images/6tqvn.jpg", Emoji.fromFormatted("<:robertify_light_blue:976955890068516924>"), Color.decode("#2bd8ff")),
    ORANGE("https://i.robertify.me/images/3svnd.png", "https://i.robertify.me/images/3lczl.jpg", "https://i.robertify.me/images/56fg5.jpg", Emoji.fromFormatted("<:robertify_orange:976955889632305152>"), Color.decode("#f25500")),
    YELLOW("https://i.robertify.me/images/xmct0.png", "https://i.robertify.me/images/szkz9.jpg", "https://i.robertify.me/images/sly4k.jpg", Emoji.fromFormatted("<:robertify_yellow:976955889829421126>"), Color.decode("#ffea2b")),
    DARK("https://i.robertify.me/images/4p6mu.png", "https://i.robertify.me/images/zu0il.jpg", "https://i.robertify.me/images/ql9ef.jpg", Emoji.fromFormatted("<:robertify_dark:976955890001403954>"), Color.decode("#0f0f0f")),
    LIGHT("https://i.robertify.me/images/n57oz.png", "https://i.robertify.me/images/wr7td.jpg", "https://i.robertify.me/images/q3rwh.jpg", Emoji.fromFormatted("<:robertify_light:976955889934290964>"), Color.decode("#f0f0f0")),
    MINT("https://i.robertify.me/images/6htkw.png", "https://i.robertify.me/images/zmtll.jpg", "https://i.robertify.me/images/g0mal.jpg", Emoji.fromFormatted("<:robertify_mint:976955889900720138>"), Color.decode("#4dffa0")),
    PASTEL_PURPLE("https://i.robertify.me/images/5hufg.png", "https://i.robertify.me/images/zxa6p.jpg", "https://i.robertify.me/images/a5ck8.jpg", Emoji.fromFormatted("<:robertify_pastel_purple:976955890508922980>"), Color.decode("#d199ff")),
    PASTEL_RED("https://i.robertify.me/images/saav0.png", "https://i.robertify.me/images/n7pr2.jpg", "https://i.robertify.me/images/yshxi.jpg", Emoji.fromFormatted("<:robertify_pastel_red:976955890542452845>"), Color.decode("#ff9999")),
    PASTEL_YELLOW("https://i.robertify.me/images/w1pe7.png", "https://i.robertify.me/images/xqu8c.jpg", "https://i.robertify.me/images/cej00.jpg", Emoji.fromFormatted("<:robertify_pastel_yellow:976955890500534293>"), Color.decode("#faff99")),
    BABY_BLUE("https://i.robertify.me/images/fants.png", "https://i.robertify.me/images/8toxc.jpg", "https://i.robertify.me/images/cetvs.jpg", Emoji.fromFormatted("<:robertify_baby_blue:976955890345336883>"), Color.decode("#99fffa"));

    companion object {
        fun parse(str: String): RobertifyTheme = when (str.lowercase()) {
            "green" -> GREEN
            "gold" -> GOLD
            "red" -> RED
            "pink" -> PINK
            "purple" -> PURPLE
            "blue" -> BLUE
            "lightblue", "light_blue" -> LIGHT_BLUE
            "orange" -> ORANGE
            "yellow" -> YELLOW
            "dark" -> DARK
            "light" -> LIGHT
            "mint" -> MINT
            "pastel_yellow" -> PASTEL_YELLOW
            "pastel_purple" -> PASTEL_PURPLE
            "pastel_red" -> PASTEL_RED
            "baby_blue" -> BABY_BLUE
            else -> throw IllegalArgumentException("Invalid theme!")
        }
    }
}