package main.commands.commands.audio.slashcommands;

import main.commands.commands.audio.SeekCommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.InteractiveCommand;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SeekSlashCommand extends InteractiveCommand {
    private final String commandName = new SeekCommand().getName();

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
                        "Jump to a specific position in the current song",
                        List.of(
                                CommandOption.of(
                                        OptionType.INTEGER,
                                        "minutes",
                                        "The minutes to seek",
                                        true
                                ),
                                CommandOption.of(
                                        OptionType.INTEGER,
                                        "seconds",
                                        "The seconds to seek",
                                        true
                                )
                        ),
                        djPredicate
                )).build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(commandName)) return;

        event.deferReply().queue();

        if (!getCommand().getCommand().permissionCheck(event)) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "You need to be a DJ to run this command!").build())
                    .queue();
            return;
        }

        final var minutes = Integer.parseInt(String.valueOf(event.getOption("minutes").getAsLong()));
        final var seconds = Integer.parseInt(String.valueOf(event.getOption("seconds").getAsLong()));
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();

        event.getHook().sendMessageEmbeds(new SeekCommand().handleSeek(selfVoiceState, memberVoiceState, minutes, seconds).build())
                .setEphemeral(false).queue();
    }
}
