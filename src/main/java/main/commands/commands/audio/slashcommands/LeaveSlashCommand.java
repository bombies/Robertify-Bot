package main.commands.commands.audio.slashcommands;

import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.commands.management.permissions.Permission;
import main.utils.GeneralUtils;
import main.utils.component.InteractiveCommand;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LeaveSlashCommand extends InteractiveCommand {
    private final String commandName = "disconnect";

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
                        "Disconnect the bot from the voice channel it's currently in",
                        List.of(),
                        List.of(),
                        null
                ))
                .build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(commandName)) return;

        EmbedBuilder eb;

        if (!GeneralUtils.hasPerms(event.getGuild(), event.getUser(), Permission.ROBERTIFY_DJ)) {
            eb  = EmbedUtils.embedMessage("You need to be a DJ to use this command!");
            event.replyEmbeds(eb.build()).queue();
            return;
        }

        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();



        if (!selfVoiceState.inVoiceChannel()) {
            eb = EmbedUtils.embedMessage("I'm already not in a voice channel!");
            event.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            eb = EmbedUtils.embedMessage("You must be in the same voice channel as me to use this command");
            event.replyEmbeds(eb.build()).queue();
            return;
        }

        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
        musicManager.scheduler.queue.clear();
        musicManager.scheduler.player.stopTrack();

        event.getGuild().getAudioManager().closeAudioConnection();

        eb = EmbedUtils.embedMessage("Disconnected!");
        event.replyEmbeds(eb.build()).queue();
    }
}
