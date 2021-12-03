package main.commands.commands.audio.slashcommands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.commands.audio.QueueCommand;
import main.utils.GeneralUtils;
import main.utils.component.InteractiveCommand;
import main.utils.pagination.Pages;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueueSlashCommand extends InteractiveCommand {
    private final String commandName = new QueueCommand().getName().toLowerCase();

    @Override
    public void initCommand() {
        setInteractionCommand(getCommand());
        upsertCommand();
    }

    @Override
    public void initCommand(Guild g) {
        setInteractionCommand(getCommand());
        upsertCommand(g);
    }

    private InteractionCommand getCommand() {
        return InteractionCommand.create()
                .setCommand(Command.of(
                        commandName,
                        "See all queued songs!",
                        djPredicate
                ))
                .build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(commandName)) return;

        event.deferReply().queue();

        if (!getCommand().getCommand().permissionCheck(event)) {
            event.getHook().sendMessageEmbeds(EmbedUtils.embedMessage("You need to be a DJ to run this command!").build())
                    .queue();
            return;
        }

        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
        final ConcurrentLinkedQueue<AudioTrack> queue = musicManager.scheduler.queue;

        GeneralUtils.setCustomEmbed("Queue");

        if (queue.isEmpty()) {
            EmbedBuilder eb = EmbedUtils.embedMessage("There is nothing in the queue.");
            event.getHook().sendMessageEmbeds(eb.build()).setEphemeral(true).queue();
            return;
        }

        var content = new QueueCommand().getContent(queue, new ArrayList<>(queue));
        Pages.paginate(content, 10, event);

        GeneralUtils.setDefaultEmbed();


    }
}
