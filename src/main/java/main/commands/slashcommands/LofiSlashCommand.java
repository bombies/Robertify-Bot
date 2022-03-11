package main.commands.slashcommands;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.commands.audio.LofiCommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.component.legacy.InteractiveCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
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
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checksWithPremium(event)) return;

        Guild guild = event.getGuild();

        if (!premiumCheck(event)) return;

        event.deferReply().queue();

        if (!musicCommandDJCheck(event)) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be a DJ to run this command!").build())
                    .queue();
            return;
        }


        try {
            event.getHook().sendMessageEmbeds(new LofiCommand().handleLofi(guild, event.getMember(), event.getTextChannel()))
                    .queue();
        } catch (IllegalArgumentException e) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Enabling Lo-Fi mode...").build())
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
                    });
        }
    }
}
