package main.utils.locale;

import main.utils.json.AbstractGuildConfig;

public class LocaleConfig extends AbstractGuildConfig {

    public void setLocale(long gid, RobertifyLocale locale) {
        final var guildObject = getGuildObject(gid);
        guildObject.put(Field.LOCALE.toString().toLowerCase(), locale.name().toLowerCase());
        getCache().updateGuild(guildObject);
    }

    public RobertifyLocale getLocale(long gid) {
        final var guildObject = getGuildObject(gid);
        if (guildObject.has(Field.LOCALE.toString().toLowerCase()))
            return RobertifyLocale.parse(guildObject.getString(Field.LOCALE.toString().toLowerCase()));
        return null;
    }

    @Override
    public void update(long gid) {

    }

    public enum Field {
        LOCALE
    }
}
