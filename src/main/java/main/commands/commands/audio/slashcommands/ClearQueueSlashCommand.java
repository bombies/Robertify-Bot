package main.commands.commands.audio.slashcommands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.commands.audio.ClearQueueCommand;
import main.constants.Permission;
import main.utils.GeneralUtils;
import main.utils.component.InteractiveCommand;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ClearQueueSlashCommand extends InteractiveCommand {
    private final String commandName = new ClearQueueCommand().getName();

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
                        "Clear the queue of all its contents",
                        e -> GeneralUtils.hasPerms(e.getGuild(), e.getUser(), Permission.ROBERTIFY_DJ)
                ))
                .build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(commandName)) return;

        event.deferReply().queue();

        final GuildMusicManager musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
        final ConcurrentLinkedQueue<AudioTrack> queue = musicManager.scheduler.queue;

        GeneralUtils.setCustomEmbed("Queue");

        if (queue.isEmpty()) {
            EmbedBuilder eb = EmbedUtils.embedMessage("There is already nothing in the queue.");
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();

        if (selfVoiceState.inVoiceChannel()) {
            if (selfVoiceState.getChannel().getMembers().size() > 2) {
                if (!getInteractionCommand().getCommand().permissionCheck(event)) {
                    EmbedBuilder eb = EmbedUtils.embedMessage("You need to be a DJ to use this command when there's other users in the channel!");
                    event.getHook().sendMessageEmbeds(eb.build()).queue();
                    return;
                }
            }
        } else {
            EmbedBuilder eb = EmbedUtils.embedMessage("The bot isn't in a voice channel.");
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        queue.clear();

        EmbedBuilder eb = EmbedUtils.embedMessage("The queue was cleared!");
        event.getHook().sendMessageEmbeds(eb.build()).queue();

        GeneralUtils.setDefaultEmbed();
    }
}
