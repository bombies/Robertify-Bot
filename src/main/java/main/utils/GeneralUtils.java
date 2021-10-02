package main.utils;

import main.commands.commands.management.permissions.Permission;
import main.constants.BotConstants;
import main.constants.ENV;
import main.main.Config;
import main.main.Robertify;
import main.utils.json.permissions.PermissionsConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

public class GeneralUtils {
    private static Color embedColor = new Color(51, 255, 0);


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
        if (sender.hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) return true;

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
                        .setTitle(BotConstants.ROBERTIFY_EMBED_TITLE.toString())
                        .setColor(embedColor)
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
                        .setColor(embedColor)
                        .setTitle(title)
                        .setFooter(Robertify.api.getSelfUser().getAsTag())
        );
    }

    public static void setCustomEmbed(String title, String footer) {
        EmbedUtils.setEmbedBuilder(
                () -> new EmbedBuilder()
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
}
