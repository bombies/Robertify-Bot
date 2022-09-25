package main.commands.slashcommands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import main.utils.resume.ResumeUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public class DisconnectSlashCommand extends AbstractSlashCommand {
    private final String commandName = "disconnect";

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName(commandName)
                        .setDescription("Disconnect the bot from the voice channel it's currently in")
                        .setPossibleDJCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Forces the bot to stop playing music and leave the voice channel" +
                " if already in one.";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;
        sendRandomMessage(event);

        event.deferReply().queue();

        EmbedBuilder eb;

        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        final Guild guild = event.getGuild();
        final GuildVoiceState selfVoiceState = guild.getSelfMember().getVoiceState();
        final var localeManager = LocaleManager.getLocaleManager(guild);

        if (!selfVoiceState.inAudioChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.DisconnectMessages.NOT_IN_CHANNEL));
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.inAudioChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention())));
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            eb = RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention())));
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);

        musicManager.leave();
        new LogUtils(guild).sendLog(LogType.BOT_DISCONNECTED, event.getUser().getAsMention() + " " + localeManager.getMessage(RobertifyLocaleMessage.DisconnectMessages.DISCONNECTED_USER));

        eb = RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.DisconnectMessages.DISCONNECTED));
        event.getHook().sendMessageEmbeds(eb.build()).queue();

        ResumeUtils.getInstance().removeInfo(guild);
    }
}
