package main.commands.slashcommands.commands.audio;

import main.commands.prefixcommands.audio.JumpCommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

public class JumpSlashCommand extends AbstractSlashCommand {
    private final String commandName = new JumpCommand().getName();

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName(commandName)
                        .setDescription("Skips the song by the given number of seconds")
                        .addOptions(
                                CommandOption.of(
                                        OptionType.INTEGER,
                                        "seconds",
                                        "Seconds to skip in the song",
                                        true
                                )
                        )
                        .setPossibleDJCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return """
                Skips the song by the given number of seconds

                Usage: `/jump <seconds_to_jump>` *(Skips the song to a specific duration)*
                """;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;
        sendRandomMessage(event);

        event.deferReply().queue();

        if (!musicCommandDJCheck(event)) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), RobertifyLocaleMessage.GeneralMessages.DJ_ONLY).build())
                    .queue();
            return;
        }

        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();

        event.getHook().sendMessageEmbeds(
                new JumpCommand().doJump(selfVoiceState, memberVoiceState, null, String.valueOf(event.getOption("seconds").getAsLong()))
                        .build()
        ).setEphemeral(true).queue();
    }
}
