package main.commands.slashcommands.commands.audio.filters;

import lavalink.client.io.filters.Vibrato;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;

public class VibratoFilter extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var guild = ctx.getGuild();
        final var msg = ctx.getMessage();
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();
        final var filters = audioPlayer.getFilters();
        final var selfMember = ctx.getSelfMember();
        final var localeManager = LocaleManager.getLocaleManager(guild);

        if (!selfMember.getVoiceState().inVoiceChannel()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.VOICE_CHANNEL_NEEDED)).build())
                    .queue();
            return;
        }

        GuildVoiceState memberVoiceState = ctx.getMember().getVoiceState();
        if (!memberVoiceState.inVoiceChannel()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL)).build())
                    .queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfMember.getVoiceState().getChannel())) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL)).build())
                    .queue();
            return;
        }

        if (filters.getVibrato() != null) {
            filters.setVibrato(null).commit();
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(
                            guild,
                            localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_MESSAGE, Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS)), Pair.of("{filter}", "Vibrato"))
                    ).build())
                    .queue();
            new LogUtils().sendLog(guild, LogType.FILTER_TOGGLE, ctx.getAuthor().getAsMention() + " " + localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_LOG_MESSAGE, Pair.of("{filter}", "Tremolo"), Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS))));
        } else {
            filters.setVibrato(new Vibrato()).commit();
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(
                            guild,
                            localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_MESSAGE, Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS)), Pair.of("{filter}", "Vibrato"))
                    ).build())
                    .queue();
            new LogUtils().sendLog(guild, LogType.FILTER_TOGGLE, ctx.getAuthor().getAsMention() + " " + localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_LOG_MESSAGE, Pair.of("{filter}", "Tremolo"), Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS))));
        }
    }

    @Override
    public String getName() {
        return "vibrato";
    }

    @Override
    public String getHelp(String prefix) {
        return "Toggle the vibrato filter";
    }

    @Override
    public boolean isPremiumCommand() {
        return true;
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("vibrato")
                        .setDescription("Toggle the vibrato filter")
                        .setPossibleDJCommand()
                        .setPremium()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Toggle the vibrato filter";
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checksWithPremium(event)) return;
        sendRandomMessage(event);

        final var guild = event.getGuild();
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();
        final var filters = audioPlayer.getFilters();
        final var selfMember = guild.getSelfMember();
        final var localeManager = LocaleManager.getLocaleManager(guild);

        if (!selfMember.getVoiceState().inVoiceChannel()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.VOICE_CHANNEL_NEEDED)).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        if (!memberVoiceState.inVoiceChannel()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL)).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfMember.getVoiceState().getChannel())) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL)).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (filters.getVibrato() != null) {
            filters.setVibrato(null).commit();
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(
                            guild,
                            localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_MESSAGE, Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS)), Pair.of("{filter}", "Vibrato"))
                    ).build())
                    .queue();
            new LogUtils().sendLog(guild, LogType.FILTER_TOGGLE, event.getUser().getAsMention() + " " + localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_LOG_MESSAGE, Pair.of("{filter}", "Tremolo"), Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS))));
        } else {
            filters.setVibrato(new Vibrato()).commit();
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(
                            guild,
                            localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_MESSAGE, Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS)), Pair.of("{filter}", "Vibrato"))
                    ).build())
                    .queue();
            new LogUtils().sendLog(guild, LogType.FILTER_TOGGLE, event.getUser().getAsMention() + " " + localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_LOG_MESSAGE, Pair.of("{filter}", "Tremolo"), Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS))));
        }
    }
}
