package main.commands.slashcommands.commands.audio;

import main.commands.prefixcommands.audio.VolumeCommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

public class VolumeSlashCommand extends AbstractSlashCommand {

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("volume")
                        .setDescription("Adjust the volume of the bot")
                        .addOptions(
                                CommandOption.of(
                                        OptionType.INTEGER,
                                        "volume",
                                        "The volume to set the bot to",
                                        true
                                )
                        )
                        .setPossibleDJCommand()
                        .setPremium()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return """
                Control the volume of the bot

                **__Usages__**
                `/volume <0-100>`""";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checksWithPremium(event)) return;
        sendRandomMessage(event);

        event.deferReply().queue();

        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();

        event.getHook().sendMessageEmbeds(new VolumeCommand().handleVolumeChange(
                selfVoiceState ,memberVoiceState,
                        GeneralUtils.longToInt(event.getOption("volume").getAsLong())
                ).build())
                .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()))
                .queue();
    }
}
