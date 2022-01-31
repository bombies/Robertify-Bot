package main.commands.commands.audio.slashcommands;

import main.audiohandlers.RobertifyAudioManager;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.InteractiveCommand;
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
                        djPredicate
                ))
                .build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(commandName)) return;

        event.deferReply().queue();

        EmbedBuilder eb;

        if (!getCommand().getCommand().permissionCheck(event)) {
            eb  = RobertifyEmbedUtils.embedMessage(event.getGuild(), "You need to be a DJ to use this command!");
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();

        if (!selfVoiceState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(event.getGuild(), "I'm already not in a voice channel!");
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(event.getGuild(), "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfVoiceState.getChannel().getAsMention());
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            eb = RobertifyEmbedUtils.embedMessage(event.getGuild(), "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfVoiceState.getChannel().getAsMention());
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());

        musicManager.leave();

        eb = RobertifyEmbedUtils.embedMessage(event.getGuild(), "Disconnected!");
        event.getHook().sendMessageEmbeds(eb.build()).queue();
    }
}
