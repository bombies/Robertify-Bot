package main.commands.commands.management.dedicatechannel;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.CommandContext;
import main.commands.commands.audio.*;
import main.commands.commands.management.toggles.togglesconfig.Toggles;
import main.commands.commands.management.toggles.togglesconfig.TogglesConfig;
import main.utils.GeneralUtils;
import main.utils.database.ServerUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class DedicatedChannelEvents extends ListenerAdapter {

    @Override
    public void onTextChannelDelete(@NotNull TextChannelDeleteEvent event) {
        final DedicatedChannelConfig config = new DedicatedChannelConfig();
        final Guild guild = event.getGuild();

        if (!config.isChannelSet(guild.getId())) return;
        if (!config.getChannelID(guild.getId()).equals(event.getChannel().getId())) return;

        config.removeChannel(guild.getId());
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        final DedicatedChannelConfig config = new DedicatedChannelConfig();
        final Guild guild = event.getGuild();

        if (!config.isChannelSet(guild.getId())) return;
        if (!config.getChannelID(guild.getId()).equals(event.getChannel().getId())) return;

        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        final User user = event.getAuthor();

        if (!user.isBot()) {
            if (!memberVoiceState.inVoiceChannel()) {
                event.getMessage().reply(user.getAsMention()).setEmbeds(EmbedUtils.embedMessage("You must be in a voice channel to use this command")
                                .build())
                        .queue();
                event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
                return;
            }

            if (selfVoiceState.inVoiceChannel())
                if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
                    event.getMessage().reply(user.getAsMention()).setEmbeds(EmbedUtils.embedMessage("You must be in the same voice channel as me to use this command")
                                    .build())
                            .queue();
                    event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
                    return;
                }
        }


        String message = event.getMessage().getContentRaw();

        if (!message.startsWith(ServerUtils.getPrefix(guild.getIdLong())) && !user.isBot()) {
            final var toggleConfig = new TogglesConfig();
            final boolean originalAnnouncementToggle = toggleConfig.getToggle(guild, Toggles.ANNOUNCE_MESSAGES);

            if (originalAnnouncementToggle) {
                toggleConfig.setToggle(guild, Toggles.ANNOUNCE_MESSAGES, false);
                config.setOriginalAnnouncementToggle(guild.getId(), originalAnnouncementToggle);
            }

            if (!GeneralUtils.isUrl(message))
                message = "ytsearch:" + message;

            PlayerManager.getInstance()
                    .loadAndPlay(event.getChannel(), message, guild.getSelfMember().getVoiceState(), event.getMember().getVoiceState(),
                            new CommandContext(event, null), null);
        }

        if (event.getAuthor().isBot()) {
            if (!event.getMessage().isEphemeral())
                event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
        } else
            event.getMessage().delete().queue();
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        if (!event.getButton().getId().startsWith(DedicatedChannelCommand.ButtonID.IDENTIFIER.toString()))
            return;

        final DedicatedChannelConfig config = new DedicatedChannelConfig();

        if (!config.isChannelSet(event.getGuild().getId())) return;
        if (!event.getTextChannel().getId().equals(config.getChannelID(event.getGuild().getId()))) return;

        final String id = event.getButton().getId();
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
        final AudioPlayer audioPlayer = musicManager.audioPlayer;
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        final User user = event.getUser();

        if (!selfVoiceState.inVoiceChannel()) {
            event.reply(user.getAsMention()).addEmbeds(EmbedUtils.embedMessage("I must be in a voice channel to do this.").build())
                    .queue();
            return;
        }

        if (!memberVoiceState.inVoiceChannel()) {
            event.reply(user.getAsMention()).addEmbeds(EmbedUtils.embedMessage("You must be in the same voice channel as me to use this command")
                    .build())
                    .queue();
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            event.reply(user.getAsMention()).addEmbeds(EmbedUtils.embedMessage("You must be in the same voice channel as me to use this command")
                            .build())
                    .queue();
        }

        if (id.equals(DedicatedChannelCommand.ButtonID.REWIND.toString())) {
            EmbedBuilder rewindEmbed = new RewindCommand().handleRewind(selfVoiceState, 0, true);
            event.reply(user.getAsMention()).addEmbeds(rewindEmbed.build())
                    .queue();
        } else if (id.equals(DedicatedChannelCommand.ButtonID.PLAY_AND_PAUSE.toString())) {
            EmbedBuilder playPauseEmbed = new PauseCommand().handlePauseEvent(event.getGuild(), selfVoiceState, memberVoiceState);
            event.reply(user.getAsMention()).addEmbeds(playPauseEmbed.build())
                    .queue();
        } else if (id.equals(DedicatedChannelCommand.ButtonID.END.toString())) {
            EmbedBuilder skipEmbed = new SkipCommand().handleSkip(selfVoiceState, memberVoiceState);
            event.reply(user.getAsMention()).addEmbeds(skipEmbed.build())
                    .queue();
        } else if (id.equals(DedicatedChannelCommand.ButtonID.LOOP.toString())) {
            EmbedBuilder loopEmbed = new LoopCommand().handleRepeat(musicManager);
            event.reply(user.getAsMention()).addEmbeds(loopEmbed.build())
                    .queue();
        } else if (id.equals(DedicatedChannelCommand.ButtonID.SHUFFLE.toString())) {
            EmbedBuilder shuffleEmbed = new ShuffleCommand().handleShuffle(event.getGuild());
            event.reply(user.getAsMention()).addEmbeds(shuffleEmbed.build())
                    .queue();
        } else if (id.equals(DedicatedChannelCommand.ButtonID.DISCONNECT.toString())) {
            EmbedBuilder disconnectEmbed = new LeaveCommand().handleDisconnect(event.getGuild(), event.getUser());
            event.reply(user.getAsMention()).addEmbeds(disconnectEmbed.build())
                    .queue();
        } else if (id.equals(DedicatedChannelCommand.ButtonID.STOP.toString())) {
            EmbedBuilder stopEmbed = new StopCommand().handleStop(musicManager);
            event.reply(user.getAsMention()).addEmbeds(stopEmbed.build())
                    .queue();
        } else if (id.equals(DedicatedChannelCommand.ButtonID.PREVIOUS.toString())) {
            EmbedBuilder previousEmbed = new PreviousTrackCommand().handlePrevious(event.getGuild(), memberVoiceState);
            event.reply(user.getAsMention()).addEmbeds(previousEmbed.build())
                    .setEphemeral(false)
                    .queue();
        }
    }
}
