package main.commands.slashcommands;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.commands.audio.QueueCommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.pagination.Pages;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class QueueSlashCommand extends AbstractSlashCommand {
    private final String commandName = new QueueCommand().getName().toLowerCase();

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName(commandName)
                        .setDescription("See all queued songs!")
                        .setPossibleDJCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!nameCheck(event)) return;
        if (!banCheck(event)) return;

        final var guild = event.getGuild();

        if (!musicCommandDJCheck(event)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You need to be a DJ to run this command!").build())
                    .queue();
            return;
        }

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
        final var queue = musicManager.getScheduler().queue;

        GeneralUtils.setCustomEmbed(event.getGuild(), "Queue");

        if (queue.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "There is nothing in the queue.");
            event.replyEmbeds(eb.build()).setEphemeral(true).queue();
            return;
        }

        var content = new QueueCommand().getContent(queue, new ArrayList<>(queue));
        Pages.paginateMessage(content, 10, event);

        GeneralUtils.setDefaultEmbed(event.getGuild());
    }
}
