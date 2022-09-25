package main.commands.slashcommands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.locale.RobertifyLocaleMessage;
import main.utils.resume.ResumeUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

public class LofiSlashCommand extends AbstractSlashCommand {

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("lofi")
                        .setDescription("Play chill beats to relax/study to")
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
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checksWithPremium(event)) return;
        sendRandomMessage(event);

        Guild guild = event.getGuild();

        if (!premiumCheck(event)) return;

        event.deferReply().queue();

        if (!musicCommandDJCheck(event)) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DJ_ONLY).build())
                    .queue();
            return;
        }


        try {
            event.getHook().sendMessageEmbeds(new LofiCommand().handleLofi(guild, event.getMember()))
                    .queue();
        } catch (IllegalArgumentException e) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LofiMessages.LOFI_ENABLING).build())
                    .queue(botMsg -> {
                        LofiCommand.getLofiEnabledGuilds().add(guild.getIdLong());
                        LofiCommand.getAnnounceLofiMode().add(guild.getIdLong());
                        RobertifyAudioManager.getInstance()
                                .loadAndPlayFromDedicatedChannel(
                                        "https://www.youtube.com/watch?v=5qap5aO4i9A&ab_channel=LofiGirl",
                                        guild.getSelfMember().getVoiceState(),
                                        event.getMember().getVoiceState(),
                                        botMsg,
                                        event,
                                        false
                                );
                        ResumeUtils.getInstance().saveInfo(guild, guild.getSelfMember().getVoiceState().getChannel());
                    });
        }
    }
}
