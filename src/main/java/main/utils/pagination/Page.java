package main.utils.pagination;

import lombok.Getter;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class Page {
    @Getter
    private final MessageEmbed embed;
    @Getter
    private final String pageContent;


    public Page(MessageEmbed embed) {
        this.embed = embed;
        this.pageContent = embed.getDescription();
    }
}
