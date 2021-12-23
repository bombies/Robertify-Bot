package main.events;

import main.utils.json.suggestions.SuggestionsConfig;
import net.dv8tion.jda.api.events.channel.category.CategoryDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class SuggestionCategoryDeletionEvents extends ListenerAdapter {

    @Override
    public void onCategoryDelete(@NotNull CategoryDeleteEvent event) {
        final var config = new SuggestionsConfig();

        if (event.getCategory().getIdLong() != config.getCategoryID()) return;

        final var guild = event.getGuild();

        guild.getTextChannelById(config.getPendingChannelID()).delete().queueAfter(1, TimeUnit.SECONDS);
        guild.getTextChannelById(config.getAcceptedChannelID()).delete().queueAfter(1, TimeUnit.SECONDS);
        guild.getTextChannelById(config.getDeniedChannelID()).delete().queueAfter(1, TimeUnit.SECONDS);

        config.resetConfig();
    }
}
