package main.commands.slashcommands.commands.audio.filters;

import lavalink.client.io.filters.Vibrato;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
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

        if (!selfMember.getVoiceState().inVoiceChannel()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "I must be in a voice channel in order for this command to work!").build())
                    .queue();
            return;
        }

        GuildVoiceState memberVoiceState = ctx.getMember().getVoiceState();
        if (!memberVoiceState.inVoiceChannel()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command").build())
                    .queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfMember.getVoiceState().getChannel())) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command").build())
                    .queue();
            return;
        }

        if (filters.getVibrato() != null) {
            filters.setVibrato(null).commit();
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have turned **off** the **Vibrato** filter").build())
                    .queue();
            new LogUtils().sendLog(guild, LogType.FILTER_TOGGLE, ctx.getAuthor().getAsMention() + " has turned the Vibrato filter off");
        } else {
            filters.setVibrato(new Vibrato()).commit();
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have turned **on** the **Vibrato** filter").build())
                    .queue();
            new LogUtils().sendLog(guild, LogType.FILTER_TOGGLE, ctx.getAuthor().getAsMention() + " has turned the Vibrato filter on");
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

        if (!selfMember.getVoiceState().inVoiceChannel()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "I must be in a voice channel in order for this command to work!").build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        if (!memberVoiceState.inVoiceChannel()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command").build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfMember.getVoiceState().getChannel())) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command").build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (filters.getVibrato() != null) {
            filters.setVibrato(null).commit();
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have turned **off** the **Vibrato** filter").build())
                    .queue();
            new LogUtils().sendLog(guild, LogType.FILTER_TOGGLE, event.getUser().getAsMention() + " has turned the Vibrato filter off");
        } else {
            filters.setVibrato(new Vibrato()).commit();
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have turned **on** the **Vibrato** filter").build())
                    .queue();
            new LogUtils().sendLog(guild, LogType.FILTER_TOGGLE, event.getUser().getAsMention() + " has turned the Vibrato filter on");
        }
    }
}
