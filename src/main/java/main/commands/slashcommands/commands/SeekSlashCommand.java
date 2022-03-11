package main.commands.slashcommands.commands;

import main.commands.commands.audio.SeekCommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.component.legacy.InteractiveCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SeekSlashCommand extends AbstractSlashCommand {

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("seek")
                        .setDescription("Jump to a specific position in the current song")
                        .addOptions(
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
                        )
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
        if (!checks(event)) return;

        event.deferReply().queue();

        if (!musicCommandDJCheck(event)) {
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
