package main.commands.prefixcommands.audio;

import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.ENV;
import main.constants.Toggles;
import main.main.Config;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import main.utils.json.restrictedchannels.RestrictedChannelsConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Deprecated @ForRemoval
public class PlayCommand implements ICommand {
    private final Logger logger = LoggerFactory.getLogger(PlayCommand.class);


    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var channel = ctx.getChannel();
        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final Member self = ctx.getSelfMember();
        final Guild guild = ctx.getGuild();
        GuildVoiceState selfVoiceState = self.getVoiceState();

        EmbedBuilder eb;

        if (args.isEmpty()) {
            final var player = RobertifyAudioManager.getInstance().getMusicManager(guild).getPlayer();

            if (player.isPaused()) {
                player.setPaused(false);
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PauseMessages.RESUMED).build())
                        .queue();
                new LogUtils(guild).sendLog(LogType.PLAYER_RESUME, RobertifyLocaleMessage.PauseMessages.RESUMED_LOG, Pair.of("{user}", ctx.getMember().getAsMention()));
                return;
            } else if (
                    ctx.getEvent().getMessage().getContentRaw()
                    .split(" ")[0]
                    .replaceFirst(GeneralUtils.toSafeString(new GuildConfig(guild).getPrefix()), "")
                    .equalsIgnoreCase("p")
            ) {
                if (player.getPlayingTrack() != null) {
                    player.setPaused(true);
                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PauseMessages.PAUSED).build())
                            .queue();
                    new LogUtils(guild).sendLog(LogType.PLAYER_RESUME, RobertifyLocaleMessage.PauseMessages.PAUSED_LOG, Pair.of("{user}", ctx.getMember().getAsMention()));
                    return;
                }
            }

            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PlayMessages.MISSING_ARGS);
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        if (!memberVoiceState.inAudioChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.USER_VOICE_CHANNEL_NEEDED);
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (selfVoiceState.inAudioChannel() && !memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()))
                    .build())
                    .queue();
            return;
        } else if (!selfVoiceState.inAudioChannel()) {
            if (TogglesConfig.getConfig(guild).getToggle(Toggles.RESTRICTED_VOICE_CHANNELS)) {
                final var restrictedChannelsConfig = new RestrictedChannelsConfig(guild);
                final var localeManager = LocaleManager.getLocaleManager(guild);
                if (!restrictedChannelsConfig.isRestrictedChannel(memberVoiceState.getChannel().getIdLong(), RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL)) {
                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.CANT_JOIN_CHANNEL) +
                                    (!restrictedChannelsConfig.getRestrictedChannels(
                                            RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL
                                    ).isEmpty()
                                            ?
                                            localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.RESTRICTED_TO_JOIN, Pair.of("{channels}", restrictedChannelsConfig.restrictedChannelsToString(
                                                    RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL
                                            )))
                                            :
                                            localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.NO_VOICE_CHANNEL)
                                    )
                            ).build())
                            .queue();
                    return;
                }
            }
        }

        if (args.get(0).equalsIgnoreCase("file")) {
            final List<Message.Attachment> attachments = msg.getAttachments();

            if (attachments.isEmpty()) {
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PlayMessages.MISSING_FILE).build())
                        .queue();
                return;
            }

            var audioFile = attachments.get(0);
            playLocalAudio(guild, channel, msg, member, audioFile);
            return;
        }

        String link;
        boolean addToBeginning = false;

        if (args.size() >= 2) {
            switch (args.get(args.size()-1).toLowerCase()) {
                case "-n", "-next" -> {
                    link = String.join(" ", args.subList(0, args.size()-1));
                    addToBeginning = true;
                }
                default -> link = String.join(" ", args.subList(0, args.size()));
            }
        } else {
            link = String.join(" ", args);
        }

        if (!GeneralUtils.isUrl(link))
            link = SpotifySourceManager.SEARCH_PREFIX + link;

        String finalLink = link;
        boolean finalAddToBeginning = addToBeginning;
        channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.FT_ADDING_TO_QUEUE_2).build()).queue(addingMsg -> {
            RobertifyAudioManager.getInstance()
                    .loadAndPlay(channel, finalLink, selfVoiceState, memberVoiceState, addingMsg, finalAddToBeginning);
        }, new ErrorHandler().handle(ErrorResponse.MISSING_PERMISSIONS, e -> RobertifyAudioManager.getInstance()
                .loadAndPlay(channel, finalLink, selfVoiceState, memberVoiceState, null, finalAddToBeginning)));
    }

    public void playLocalAudio(Guild guild, GuildMessageChannel channel, Message msg, Member member, Message.Attachment audioFile) {
        switch (audioFile.getFileExtension().toLowerCase()) {
            case "mp3", "ogg", "m4a", "wav", "flac", "webm", "mp4", "aac", "mov" -> {
                if (!Files.exists(Path.of(Config.get(ENV.AUDIO_DIR) + "/"))) {
                    try {
                        Files.createDirectories(Paths.get(Config.get(ENV.AUDIO_DIR)));
                    } catch (Exception e) {
                        msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PlayMessages.LOCAL_DIR_ERR).build())
                                .setActionRow(Button.of(ButtonStyle.LINK, "https://robertify.me/support", "Support Server"))
                                .queue();

                        logger.error("[FATAL ERROR] Could not create audio directory!", e);
                        return;
                    }
                }

                final var selfVoiceState = guild.getSelfMember().getVoiceState();
                final var memberVoiceState = member.getVoiceState();

                try {
                    if (!Files.exists(Path.of(Config.get(ENV.AUDIO_DIR) + "/" + audioFile.getFileName()))) {
                        audioFile.downloadToFile(Config.get(ENV.AUDIO_DIR) + "/" + audioFile.getFileName())
                                .thenAccept(file -> {
                                    try {
                                        file.createNewFile();
                                    } catch (IOException e) {
                                        logger.error("[FATAL ERROR] Error when trying to create a new audio file!", e);
                                    }

                                    channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.FT_ADDING_TO_QUEUE_2).build()).queue(addingMsg -> {
                                        RobertifyAudioManager.getInstance()
                                                .loadAndPlayLocal(channel, file.getPath(), selfVoiceState, memberVoiceState, addingMsg, false);
                                    });
                                })
                                .exceptionally(e -> {
                                    logger.error("[FATAL ERROR] Error when attempting to download track", e);
                                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PlayMessages.FILE_DOWNLOAD_ERR).build())
                                            .setActionRow(Button.of(ButtonStyle.LINK, "https://robertify.me/support", "Support Server"))
                                            .queue();
                                    return null;
                                });
                    } else {
                        File localAudioFile = new File(Config.get(ENV.AUDIO_DIR) + "/" + audioFile.getFileName());
                        channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.FT_ADDING_TO_QUEUE_2).build()).queue(addingMsg -> {
                            RobertifyAudioManager.getInstance()
                                    .loadAndPlayLocal(channel, localAudioFile.getPath(), selfVoiceState, memberVoiceState, addingMsg, false);
                        }, new ErrorHandler().handle(ErrorResponse.MISSING_PERMISSIONS, e -> RobertifyAudioManager.getInstance()
                                .loadAndPlayLocal(channel, localAudioFile.getPath(), selfVoiceState, memberVoiceState, null, false)));
                    }
                } catch (IllegalArgumentException e) {
                    logger.error("[FATAL ERROR] Error when attempting to download track", e);
                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PlayMessages.FILE_DOWNLOAD_ERR).build())
                            .setActionRow(Button.of(ButtonStyle.LINK, "https://robertify.me/support", "Support Server"))
                            .queue();
                }
            }
            default -> msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PlayMessages.INVALID_FILE).build())
                    .queue();
        }
    }

    @Override
    public String getName() {
        return "play";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+ GeneralUtils.listToString(getAliases())+"`\n" +
                "Plays a song\n\n" +
                "**__Usages__**\n" +
                "`" + prefix + "play <song>`\n" +
                "`"+ prefix +"play <song> -next` *(Add tracks to the beginning of the queue)*\n" +
                "`"+ prefix +"play file` *(Must have a file attached to the message)*";
    }

    @Override
    public List<String> getAliases() {
        return List.of("p", "rob");
    }
}
