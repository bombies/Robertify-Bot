package main.utils.locale;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

public enum RobertifyLocale {
    ENGLISH("en", "English (UK)", "English (UK)", "\uD83C\uDDEC\uD83C\uDDE7"),
    SPANISH("es", "Spanish", "Español", "\uD83C\uDDEA\uD83C\uDDF8"),
    PORTUGUESE("pt", "Portuguese", "Português", "\uD83C\uDDF5\uD83C\uDDF9"),
    RUSSIAN("ru", "Russian", "Русский", "\uD83C\uDDF7\uD83C\uDDFA"),
    DUTCH("nl", "Dutch", "Nederlands", "\uD83C\uDDF3\uD83C\uDDF1"),
    FRENCH("fr", "French", "Français", "\uD83C\uDDEB\uD83C\uDDF7"),
    GERMAN("de", "German", "Deutsch", "\uD83C\uDDE9\uD83C\uDDEA");

    @Getter
    private final String code;
    @Getter
    private final String name;
    @Getter
    private final String localName;
    @Getter
    private final String flag;

    RobertifyLocale(String code, String name, String localName, String flag) {
        this.code = code;
        this.name = name;
        this.localName = localName;
        this.flag = flag;
    }

    public static List<RobertifyLocale> getAvailableLanguages() {
        return Arrays.stream(RobertifyLocale.values()).toList();
    }

    public static RobertifyLocale parse(String locale) {
        switch (locale.toUpperCase()) {
            case "ENGLISH", "EN" -> {
                return ENGLISH;
            }
            case "SPANISH", "ES" -> {
                return SPANISH;
            }
            case "PORTUGUESE", "PT" -> {
                return PORTUGUESE;
            }
            case "RUSSIAN", "RU" -> {
                return RUSSIAN;
            }
            case "DUTCH", "NL" -> {
                return DUTCH;
            }
            case "GERMAN", "DE" -> {
                return GERMAN;
            }
            case "FRENCH", "FR" -> {
                return FRENCH;
            }
            default -> throw new IllegalArgumentException("There is no such locale with the name: " + locale);
        }
    }
}
