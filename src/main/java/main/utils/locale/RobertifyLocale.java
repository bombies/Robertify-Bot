package main.utils.locale;

import lombok.Getter;

public enum RobertifyLocale {
    ENGLISH("en"),
    SPANISH("es"),
    PORTUGUESE("pt"),
    RUSSIAN("ru"),
    DUTCH("nl");

    @Getter
    private final String code;

    RobertifyLocale(String code) {
        this.code = code;
    }
}
