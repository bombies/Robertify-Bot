package main.commands.slashcommands.commands.audio;

import main.commands.prefixcommands.audio.SeekCommand;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

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
                                        "hours",
                                        "The hours to seek",
                                        true
                                ),
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
        sendRandomMessage(event);

        event.deferReply().queue();

        final var hours = Integer.parseInt(String.valueOf(event.getOption("hours").getAsLong()));
        final var minutes = Integer.parseInt(String.valueOf(event.getOption("minutes").getAsLong()));
        final var seconds = Integer.parseInt(String.valueOf(event.getOption("seconds").getAsLong()));
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();

        event.getHook().sendMessageEmbeds(new SeekCommand().handleSeek(selfVoiceState, memberVoiceState, hours, minutes, seconds).build())
                .setEphemeral(false).queue();
    }
}
