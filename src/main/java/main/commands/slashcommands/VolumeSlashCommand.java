package main.commands.slashcommands;

import main.commands.commands.audio.VolumeCommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
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
        return null;
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!nameCheck(event)) return;
        if (!premiumCheck(event)) return;

        event.deferReply().queue();

        if (!musicCommandDJCheck(event)) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "You need to be a DJ to run this command!").build())
                    .queue();
            return;
        }

        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();

        event.getHook().sendMessageEmbeds(new VolumeCommand().handleVolumeChange(
                selfVoiceState ,memberVoiceState,
                        GeneralUtils.longToInt(event.getOption("volume").getAsLong())
                ).build())
                .setEphemeral(false)
                .queue();
    }
}
