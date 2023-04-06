package main.commands.slashcommands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.locale.RobertifyLocaleMessage;
import main.utils.pagination.PaginationHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

public class QueueSlashCommand extends AbstractSlashCommand {

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("queue")
                        .setDescription("See all queued songs!")
                        .setPossibleDJCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Shows all the queued songs";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;
        sendRandomMessage(event);

        final var guild = event.getGuild();
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
        final var queueHandler = musicManager.getScheduler().getQueueHandler();

        if (queueHandler.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NOTHING_IN_QUEUE);
            event.replyEmbeds(eb.build()).queue();
            return;
        }

        PaginationHandler.paginateQueue(10, event);
    }
}
