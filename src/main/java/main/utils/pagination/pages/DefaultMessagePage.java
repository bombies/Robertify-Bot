package main.utils.pagination.pages;

import lombok.Getter;
import main.utils.RobertifyEmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;

import javax.annotation.Nullable;
import java.util.List;

public class DefaultMessagePage extends MessagePage {
    @Getter
    private final MessageEmbed embed;

    public DefaultMessagePage(@Nullable Guild guild, List<String> content) {
        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "\t");
        for (String str : content)
            eb.appendDescription(str + "\n");
        embed = eb.build();
    }

    public DefaultMessagePage(List<String> content) {
        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage("\t");
        for (String str : content)
            eb.appendDescription(str + "\n");
        embed = eb.build();
    }

    public DefaultMessagePage(MessageEmbed embed) {
        this.embed = embed;
    }
}
