package main.commands.slashcommands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.audiohandlers.TrackScheduler;
import main.commands.prefixcommands.audio.QueueCommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.locale.RobertifyLocaleMessage;
import main.utils.pagination.Pages;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

public class HistoryCommand extends AbstractSlashCommand {
    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("history")
                        .setDescription("See all the songs that have been played in your current listening session")
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;

        final var guild = event.getGuild();
        final var scheduler = RobertifyAudioManager.getInstance().getMusicManager(guild).getScheduler();
        final var queueHandler = scheduler.getQueueHandler();

        if (queueHandler.isPreviousTracksEmpty()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.HistoryMessages.NO_PAST_TRACKS).build())
                    .queue();
        } else {
            final var content = new QueueCommand().getPastTrackContent(guild, queueHandler);
            Pages.paginateMessage(content, 10, event);
        }
    }
}
