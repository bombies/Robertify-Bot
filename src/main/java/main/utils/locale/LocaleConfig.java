package main.utils.locale;

import main.utils.json.AbstractGuildConfig;
import net.dv8tion.jda.api.entities.Guild;

public class LocaleConfig extends AbstractGuildConfig {
    private final Guild guild;
    private final long gid;

    public LocaleConfig(Guild guild) {
        super(guild);
        this.guild = guild;
        this.gid = guild.getIdLong();
    }

    public void setLocale(RobertifyLocale locale) {
        final var guildObject = getGuildObject();
        guildObject.put(Field.LOCALE.toString().toLowerCase(), locale.name().toLowerCase());
        getCache().updateGuild(guildObject);
    }

    public RobertifyLocale getLocale() {
        final var guildObject = getGuildObject();
        if (guildObject.has(Field.LOCALE.toString().toLowerCase()))
            return RobertifyLocale.parse(guildObject.getString(Field.LOCALE.toString().toLowerCase()));
        return null;
    }

    @Override
    public void update() {

    }

    public enum Field {
        LOCALE
    }
}
