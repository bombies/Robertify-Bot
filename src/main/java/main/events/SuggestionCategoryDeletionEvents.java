package main.events;

import main.utils.database.mongodb.cache.BotBDCache;
import net.dv8tion.jda.api.events.channel.category.CategoryDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class SuggestionCategoryDeletionEvents extends ListenerAdapter {

    @Override
    public void onCategoryDelete(@NotNull CategoryDeleteEvent event) {
        final var category = event.getCategory();
        final var config = BotBDCache.getInstance();

        if (category.getIdLong() != config.getSuggestionsCategoryID()) return;

        final var guild = event.getGuild();

        guild.getTextChannelById(config.getSuggestionsPendingChannelID()).delete().queueAfter(1, TimeUnit.SECONDS);
        guild.getTextChannelById(config.getSuggestionsAcceptedChannelID()).delete().queueAfter(1, TimeUnit.SECONDS);
        guild.getTextChannelById(config.getSuggestionsDeniedChannelID()).delete().queueAfter(1, TimeUnit.SECONDS);

        config.resetSuggestionsConfig();
    }
}
