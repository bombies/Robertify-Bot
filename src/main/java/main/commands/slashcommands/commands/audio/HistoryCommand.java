package main.commands.slashcommands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.audiohandlers.TrackScheduler;
import main.commands.prefixcommands.audio.QueueCommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.locale.RobertifyLocaleMessage;
import main.utils.pagination.Pages;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
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
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checks(event)) return;

        final var guild = event.getGuild();
        final var pastTracks = TrackScheduler.getPastQueue().get(event.getGuild().getIdLong());
        GeneralUtils.setCustomEmbed(guild, RobertifyLocaleMessage.HistoryMessages.HISTORY_EMBED_TITLE);

        if (pastTracks == null) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.HistoryMessages.NO_PAST_TRACKS).build())
                    .queue();
            return;
        }

        if (pastTracks.empty()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.HistoryMessages.NO_PAST_TRACKS).build())
                    .queue();
        } else {
            final var content = new QueueCommand().getContent(guild, pastTracks);
            Pages.paginateMessage(content, 10, event);
        }

        GeneralUtils.setDefaultEmbed(guild);
    }
}
