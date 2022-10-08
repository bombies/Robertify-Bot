package main.utils.locale;

import lombok.Getter;
import lombok.SneakyThrows;
import main.main.Robertify;
import main.utils.GeneralUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocaleManager {
    private final static Logger logger = LoggerFactory.getLogger(LocaleManager.class);
    private final static HashMap<Long, LocaleManager> localeManagers = new HashMap<>();
    private final static HashMap<RobertifyLocale, Map<String, String>> locales = new HashMap<>();

    @Getter
    private RobertifyLocale locale;
    private Map<String, String> localeFile;
    private final Guild guild;
    private final long guildID;

    private LocaleManager(Guild guild, RobertifyLocale locale) {
        locales.putIfAbsent(locale, retrieveLocaleFile(locale));
        this.guild = guild;
        this.guildID = guild.getIdLong();
        this.locale = locale;
        this.localeFile = locales.get(locale);
    }

    public static LocaleManager getLocaleManager(Guild guild) {
        if (!localeManagers.containsKey(guild.getIdLong()))
            localeManagers.put(guild.getIdLong(), new LocaleManager(guild, RobertifyLocale.ENGLISH));
        return localeManagers.get(guild.getIdLong());
    }


    public void setLocale(RobertifyLocale locale) {
        locales.putIfAbsent(locale, retrieveLocaleFile(locale));

        this.locale = locale;
        this.localeFile = retrieveLocaleFile();
        new LocaleConfig(guild).setLocale(locale);
    }

    public static void reloadLocales() {
        locales.clear();
        RobertifyLocale.getAvailableLanguages()
                .forEach(locale -> locales.put(locale, retrieveLocaleFile(locale)));
        Robertify.getShardManager().getGuildCache().forEach(guild -> {
            if (localeManagers.containsKey(guild.getIdLong())) {
                LocaleManager localeManager = localeManagers.get(guild.getIdLong());
                localeManager.setLocale(localeManager.locale);
            }
        });
    }

    public String getMessage(LocaleMessage message) {
        return localeFile.get(message.name().toLowerCase());
    }

    @SafeVarargs
    public final String getMessage(LocaleMessage message, Pair<String, String>... placeholders) {
        String msg = localeFile.get(message.name().toLowerCase());

        if (placeholders.length != 0)
            for (final var placeholder : placeholders)
                msg = msg.replaceAll(Pattern.quote(placeholder.getLeft()), Matcher.quoteReplacement(placeholder.getRight()));
        return msg;
    }

    @SneakyThrows
    private Map<String, String> retrieveLocaleFile() {
        return retrieveLocaleFile(locale);
    }

    @SneakyThrows
    private static Map<String, String> retrieveLocaleFile(RobertifyLocale locale) {
        if (!GeneralUtils.directoryExists("./locale")) {
            GeneralUtils.createDirectory("./locale");
            logger.error("There was no file found for locale: " + locale.getCode().toUpperCase());
            createAllLocaleFiles();
            System.exit(-1);
        }

        File localeFile = new File("./locale/messages." + locale.getCode().toLowerCase() + ".yml");

        if (!localeFile.exists())
            createLocaleFile(locale);

//        if (localeFile.length() == 0) {
//            logger.error("There was no information found in the file for locale: " + locale.getCode().toUpperCase());
//            System.exit(-1);
//        }

        Map<String, String> fileMap = new Yaml().load(GeneralUtils.getFileContent(localeFile.getPath()));
        return checkLocaleFile(localeFile, fileMap);
    }

    @SneakyThrows
    private void createLocaleFile() {
        createLocaleFile(locale);
    }

    @SneakyThrows
    private static void createLocaleFile(RobertifyLocale locale) {
        final File file = new File("./locale/messages." + locale.getCode().toLowerCase() + ".yml");
        file.createNewFile();

        StringBuilder content = new StringBuilder();
        for (final var fieldSection : RobertifyLocaleMessage.getMessageTypes().values())
            for (final var field : fieldSection)
                content.append(field.name().toLowerCase()).append(": ").append("\"Fill me out!\"\n");
        GeneralUtils.setFileContent(file, content.toString());
    }

    private static void createAllLocaleFiles() {
        for (final var locale : RobertifyLocale.values())
            createLocaleFile(locale);
    }

    private static Map<String, String> checkLocaleFile(File localeFile, Map<String, String> fileMap) {
        StringBuilder contentToAppend = new StringBuilder();
        for (final var fieldSection : RobertifyLocaleMessage.getMessageTypes().values()) {
            for (final var field : fieldSection)
                if (!fileMap.containsKey(field.name().toLowerCase())) {
                    contentToAppend.append(field.name().toLowerCase()).append(": \"Fill me out!\"\n");
                    fileMap.put(field.name().toLowerCase(), "Fill me out!");
                }
        }

        GeneralUtils.appendFileContent(localeFile, contentToAppend.toString());
        return fileMap;
    }
}
