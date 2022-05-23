package main.utils.locale;

import lombok.Getter;
import lombok.SneakyThrows;
import main.utils.GeneralUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LocaleManager {
    private final static Logger logger = LoggerFactory.getLogger(LocaleManager.class);
    private final static HashMap<Long, LocaleManager> localeManagers = new HashMap<>();

    @Getter
    private RobertifyLocale locale;
    private Map<String, String> localeFile;

    private LocaleManager(RobertifyLocale locale) {
        this.locale = locale;
        this.localeFile = retrieveLocaleFile();
    }

    public static LocaleManager getLocaleManager(long guildID) {
        localeManagers.putIfAbsent(guildID, new LocaleManager(RobertifyLocale.ENGLISH));
        return localeManagers.get(guildID);
    }

    public void setLocale(RobertifyLocale locale) {
        this.locale = locale;
        this.localeFile = retrieveLocaleFile();
    }

    public String getMessage(RobertifyLocaleMessage message) {
        checkLocaleFile(new File("./locale/messages." + locale.getCode().toLowerCase() + ".yml"), localeFile);
        return localeFile.get(message.name().toLowerCase());
    }

    @SneakyThrows
    private Map<String, String> retrieveLocaleFile() {
        if (!GeneralUtils.directoryExists("./locale")) {
            GeneralUtils.createDirectory("./locale");
            logger.error("There was no file found for locale: " + locale.getCode().toUpperCase());
            createAllLocaleFiles();
            System.exit(-1);
        }

        File localeFile = new File("./locale/messages." + locale.getCode().toLowerCase() + ".yml");

        if (!localeFile.exists()) {
            logger.error("There was no information found in the file for locale: " + locale.getCode().toUpperCase());
            createLocaleFile();
            System.exit(-1);
        }

        if (localeFile.length() == 0) {
            logger.error("There was no information found in the file for locale: " + locale.getCode().toUpperCase());
            System.exit(-1);
        }

        Map<String, String> fileMap = new Yaml().load(GeneralUtils.getFileContent(localeFile.getPath()));
        return checkLocaleFile(localeFile, fileMap);
    }

    @SneakyThrows
    private void createLocaleFile() {
        final File file = new File("./locale/messages." + locale.getCode().toLowerCase() + ".yml");
        file.createNewFile();

        StringBuilder content = new StringBuilder();
        for (final var field : RobertifyLocaleMessage.values())
            content.append(field.name().toLowerCase()).append(": ").append("\"Fill me out!\"\n");
        GeneralUtils.setFileContent(file, content.toString());
    }

    @SneakyThrows
    private void createLocaleFile(RobertifyLocale locale) {
        final File file = new File("./locale/messages." + locale.getCode().toLowerCase() + ".yml");
        file.createNewFile();

        StringBuilder content = new StringBuilder();
        for (final var field : RobertifyLocaleMessage.values())
            content.append(field.name().toLowerCase()).append(": ").append("\"Fill me out!\"\n");
        GeneralUtils.setFileContent(file, content.toString());
    }

    private void createAllLocaleFiles() {
        for (final var locale : RobertifyLocale.values())
            createLocaleFile(locale);
    }

    private Map<String, String> checkLocaleFile(File localeFile, Map<String, String> fileMap) {
        StringBuilder contentToAppend = new StringBuilder();
        for (final var field : RobertifyLocaleMessage.values())
            if (!fileMap.containsKey(field.name().toLowerCase())) {
                contentToAppend.append(field.name().toLowerCase()).append(": \"Fill me out!\"\n");
                fileMap.put(field.name().toLowerCase(), "Fill me out!");
            }
        GeneralUtils.appendFileContent(localeFile, contentToAppend.toString());
        return fileMap;
    }
}
