package main.utils;

import main.commands.commands.management.permissions.Permission;
import main.constants.BotConstants;
import main.constants.ENV;
import main.constants.TimeFormat;
import main.main.Config;
import main.utils.json.permissions.PermissionsConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.annotations.ReplaceWith;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class GeneralUtils {
    private static Color embedColor = parseColor(Config.get(ENV.BOT_COLOR));


    public static String getEmojiRegex() {
        return "([\\u20a0-\\u32ff\\ud83c\\udc00-\\ud83d\\udeff\\udbb9\\udce5-\\udbb9\\udcee])";
    }

    public static boolean stringIsNum(String s) {
        if (s == null) return false;
        else {
            try {
                double d = Double.parseDouble(s);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    public static boolean stringIsInt(String s) {
        if (s == null) return false;
        else {
            try {
                int d = Integer.parseInt(s);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    public static int longToInt(long l) {
        if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE)
            throw new IllegalArgumentException("This long exceeds the integer limits!");

        return Integer.parseInt(String.valueOf(l));
    }

    public static boolean stringIsID(String s) {
        String idRegex = "^[0-9]{18}$";
        return Pattern.matches(idRegex, s);
    }

    public static boolean isUrl(String url) {
        try {
            new URI(url);
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public static String getDigitsOnly(String s) {
        return s.replaceAll("\\D", "");
    }

    public static void updateENVField(ENV field, String str) throws IOException {
        switch (field) {
            case BOT_TOKEN -> throw new IllegalAccessError("This env value can't be changed from the bot!");
            default -> {
                int doNothing;
            }
        }
        String fileContent = getFileContent(".env");
        String envFieldTitle = field.name();
        String envFieldValue = Config.get(field);
        setFileContent(
                ".env",
                fileContent.replace(envFieldTitle+"="+envFieldValue, envFieldTitle+"="+str)
        );
        Config.reload();
    }

    public static boolean  hasPerms(Guild guild, Member sender, Permission perm) {
        if (sender.hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)
        || sender.isOwner()) return true;

        List<Role> userRoles = sender.getRoles();

        PermissionsConfig permissionsConfig = new PermissionsConfig();

        for (Role r : userRoles)
            if (permissionsConfig.getRolesForPermission(guild.getId(), perm).contains(r.getId()) ||
                    permissionsConfig.getRolesForPermission(guild.getId(), Permission.ROBERTIFY_ADMIN).contains(r.getId()))
                return true;
        return false;
    }

    public static boolean hasPerms(Guild guild, User sender, Permission perm) {
        return hasPerms(guild, guild.getMember(sender), perm);
    }

    public static boolean hasPerms(Guild guild, Member sender, Permission... perms) {
        if (sender.hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) return true;

        List<Role> userRoles = sender.getRoles();
        int pass = 0;

        PermissionsConfig permissionsConfig = new PermissionsConfig();


        for (Role r : userRoles) {
            if (permissionsConfig.getRolesForPermission(guild.getId(), Permission.ROBERTIFY_ADMIN).contains(r.getId()))
                return true;
            for (Permission p : perms)
                if (permissionsConfig.getRolesForPermission(guild.getId(), p).contains(r.getId()))
                    pass++;
        }
        return pass == perms.length;
    }

    public static boolean hasPerms(Guild guild, User sender, Permission... perm) {
        return hasPerms(guild, guild.getMember(sender), perm);
    }

    @Deprecated @ForRemoval
    @ReplaceWith("progressBar(String percent, ProgressBar barType)")
    public static String progressBar(double percent) {
        StringBuilder str = new StringBuilder();
        for(int i=0; i<12; i++)
            if(i == (int)(percent*12))
                str.append("\uD83D\uDD18"); // 🔘
            else
                str.append("▬");
        return str.toString();
    }

    public static String progressBar(double percent, ProgressBar barType) {
        switch (barType) {
            case DURATION -> {
                StringBuilder str = new StringBuilder();
                for(int i=0; i<12; i++)
                    if(i == (int)(percent*12))
                        str.append("\uD83D\uDD18"); // 🔘
                    else
                        str.append("▬");
                return str.toString();
            }
            case FILL -> {
                StringBuilder str = new StringBuilder();
                for(int i=0; i<12; i++)
                    if(i <= (int)(percent*12))
                        str.append("█");
                    else
                        str.append("▒");
                return str.toString();
            }
        }
        throw new NullPointerException("Something went wrong!");
    }

    public enum ProgressBar {
        DURATION,
        FILL
    }

    public static String trimString(String string, String delimiter) {
        if (!string.contains(delimiter)) return string;

        switch (delimiter) {
            case "?", "^", "[", ".", "$", "{", "&", "(", "+", ")", "|", "<", ">", "]", "}"
                -> delimiter = "\\\\" + delimiter;
        }
        System.out.println(delimiter+"[a-zA-Z0-9~!@#$%^&*()\\-_=;:'\"|\\\\,./]*");

        return string.replaceAll(delimiter+"[a-zA-Z0-9~!@#$%^&*()\\-_=;:'\"|\\\\,./]*", "");
    }

    public static String getJoinedString(List<String> args, int startIndex) {
        StringBuilder arg = new StringBuilder();
        for (int i = startIndex; i < args.size(); i++)
            arg.append(args.get(i)).append((i < args.size() - 1) ? " " : "");
        return  arg.toString();
    }

    public static String formatDate(long date, TimeFormat style) {
        return switch (style) {
            case DD_MMMM_YYYY, MM_DD_YYYY, DD_MMMM_YYYY_ZZZZ, DD_M_YYYY_HH_MM_SS,
                    E_DD_MMM_YYYY_HH_MM_SS_Z -> new SimpleDateFormat(style.toString()).format(date);
            default -> throw new IllegalArgumentException("The enum provided isn't a supported enum!");
        };
    }

    public static String formatTime(long duration) {
        final long hours = duration / TimeUnit.HOURS.toMillis(1);
        final long minutes = duration / TimeUnit.MINUTES.toMillis(1);
        final long seconds = duration % TimeUnit.MINUTES.toMillis(1) / TimeUnit.SECONDS.toMillis(1);

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static String getFileContent(String path) {
        String ret = null;
        try {
            ret = new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (ret == null) throw new NullPointerException();

        return ret.replaceAll("\t\n", "");
    }

    public static String getFileContent(Path path) {
        String ret = null;
        try {
            ret = new String(Files.readAllBytes(path));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (ret == null) throw new NullPointerException();

        return ret.replaceAll("\t\n", "");
    }

    public static void setFileContent(String path, String content) throws IOException {
        File file = new File(path);
        if (!file.exists())
            file.createNewFile();

        FileWriter writer = new FileWriter(path, false);
        writer.write(content);
        writer.close();
    }

    public static void setFileContent(Path path, String content) throws IOException {
        File file = new File(String.valueOf(path));
        if (!file.exists())
            file.createNewFile();

        FileWriter writer = new FileWriter(String.valueOf(path), false);
        writer.write(content);
        writer.close();
    }

    public static void setDefaultEmbed() {
        EmbedUtils.setEmbedBuilder(
                () -> new EmbedBuilder()
                        .setColor(embedColor)
                        .setAuthor(BotConstants.ROBERTIFY_EMBED_TITLE.toString(), null, BotConstants.ICON_URL.toString())
        );
    }

    public static void setCustomEmbed(Color color) {
        EmbedUtils.setEmbedBuilder(
                () -> new EmbedBuilder()
                        .setColor(color)
        );
    }

    public static void setCustomEmbed(String author, Color color) {
        EmbedUtils.setEmbedBuilder(
                () -> new EmbedBuilder()
                        .setColor(color)
                        .setAuthor(author, null, BotConstants.ICON_URL.toString())
        );
    }

    public static void setCustomEmbed(String author) {
        EmbedUtils.setEmbedBuilder(
                () -> new EmbedBuilder()
                        .setColor(embedColor)
                        .setAuthor(author, null, BotConstants.ICON_URL.toString())
        );
    }

    public static void setCustomEmbed(String author, String footer) {
        EmbedUtils.setEmbedBuilder(
                () -> new EmbedBuilder()
                        .setColor(embedColor)
                        .setAuthor(author,null, BotConstants.ICON_URL.toString())
                        .setFooter(footer)
        );
    }

    public static void setCustomEmbed(String author, @Nullable String title, String footer) {
        EmbedUtils.setEmbedBuilder(
                () -> new EmbedBuilder()
                        .setAuthor(author, null, BotConstants.ICON_URL.toString())
                        .setColor(embedColor)
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

    public static Color parseColor(String hex) {
        return Color.decode(hex);
    }
}
