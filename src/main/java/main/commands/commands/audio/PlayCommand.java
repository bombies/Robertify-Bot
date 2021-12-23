package main.commands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.commands.commands.management.toggles.togglesconfig.Toggles;
import main.commands.commands.management.toggles.togglesconfig.TogglesConfig;
import main.constants.ENV;
import main.main.Config;
import main.utils.GeneralUtils;
import main.utils.database.sqlite3.BotDB;
import main.utils.database.sqlite3.ServerDB;
import main.utils.json.restrictedchannels.RestrictedChannelsConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class PlayCommand implements ICommand {
    private final Logger logger = LoggerFactory.getLogger(PlayCommand.class);


    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final TextChannel channel = ctx.getChannel();
        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final Member self = ctx.getSelfMember();
        GuildVoiceState selfVoiceState = self.getVoiceState();

        EmbedBuilder eb;

        BotDB botUtils = new BotDB();
        if (!botUtils.isAnnouncementChannelSet(ctx.getGuild().getIdLong())) {
            botUtils.createConnection();
            botUtils.setAnnouncementChannel(ctx.getGuild().getIdLong(), ctx.getChannel().getIdLong())
                    .closeConnection();

            eb = EmbedUtils.embedMessage("There was no announcement channel set! Setting it to this channel.\n" +
                    "\n_You can change the announcement channel by using the \"setchannel\" command._");
            ctx.getChannel().sendMessageEmbeds(eb.build()).queue();
        }

        if (args.isEmpty()) {
            eb = EmbedUtils.embedMessage("You must provide the name or link of a song to play!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        if (!memberVoiceState.inVoiceChannel()) {
            eb = EmbedUtils.embedMessage("You need to be in a voice channel for this to work");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (selfVoiceState.inVoiceChannel() && !memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            msg.replyEmbeds(EmbedUtils.embedMessage("You must be in the same voice channel as me to use this command!")
                    .build())
                    .queue();
            return;
        } else if (!selfVoiceState.inVoiceChannel()) {
            if (new TogglesConfig().getToggle(ctx.getGuild(), Toggles.RESTRICTED_VOICE_CHANNELS)) {
                final var restrictedChannelsConfig = new RestrictedChannelsConfig();
                if (!restrictedChannelsConfig.isRestrictedChannel(ctx.getGuild().getId(), memberVoiceState.getChannel().getIdLong(), RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL)) {
                    msg.replyEmbeds(EmbedUtils.embedMessage("I can't join this channel!" +
                                    "\n\nI am restricted to only join\n"
                                    + restrictedChannelsConfig.restrictedChannelsToString(ctx.getGuild().getId(), RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL)).build())
                            .queue();
                    return;
                }
            }
        }

        if (args.get(0).equalsIgnoreCase("file")) {
            final List<Message.Attachment> attachments = msg.getAttachments();

            if (attachments.isEmpty()) {
                msg.replyEmbeds(EmbedUtils.embedMessage("You must attach an audio file to play!").build())
                        .queue();
                return;
            }

            var audioFile = attachments.get(0);

            switch (audioFile.getFileExtension().toLowerCase()) {
                case "mp3", "ogg", "m4a", "wav", "flac" -> {
                    if (!Files.exists(Path.of(Config.get(ENV.AUDIO_DIR) + "/"))) {
                        try {
                            Files.createDirectories(Paths.get(Config.get(ENV.AUDIO_DIR)));
                        } catch (Exception e) {
                            msg.replyEmbeds(EmbedUtils.embedMessage("Something went wrong when attempting to create a " +
                                    "local audio directory. Contact the developers immediately!").build())
                                    .queue();

                            logger.error("[FATAL ERROR] Could not create audio directory!", e);
                            return;
                        }
                    }

                    try {
                        if (!Files.exists(Path.of(Config.get(ENV.AUDIO_DIR) + "/" + audioFile.getFileName()))) {
                            audioFile.downloadToFile(Config.get(ENV.AUDIO_DIR) + "/" + audioFile.getFileName())
                                    .thenAccept(file -> {
                                        try {
                                            file.createNewFile();
                                        } catch (IOException e) {
                                            logger.error("[FATAL ERROR] Error when trying to create a new audio file!", e);
                                        }

                                        channel.sendMessageEmbeds(EmbedUtils.embedMessage("Adding to queue...").build()).queue(addingMsg -> {
                                            RobertifyAudioManager.getInstance()
                                                    .loadAndPlayLocal(channel, file.getPath(), selfVoiceState, memberVoiceState, ctx, addingMsg);
                                        });
                                    })
                                    .exceptionally(e -> {
                                        logger.error("[FATAL ERROR] Error when attempting to download track", e);
                                        msg.replyEmbeds(EmbedUtils.embedMessage("Something went wrong when attempting to " +
                                                        "download the file. Contact the developers immediately!").build())
                                                .queue();
                                        return null;
                                    });
                        } else {
                            File localAudioFile = new File(Config.get(ENV.AUDIO_DIR) + "/" + audioFile.getFileName());
                            channel.sendMessageEmbeds(EmbedUtils.embedMessage("Adding to queue...").build()).queue(addingMsg -> {
                                RobertifyAudioManager.getInstance()
                                        .loadAndPlayLocal(channel, localAudioFile.getPath(), selfVoiceState, memberVoiceState, ctx, addingMsg);
                            });
                        }
                    } catch (IllegalArgumentException e) {
                        logger.error("[FATAL ERROR] Error when attempting to download track", e);
                        msg.replyEmbeds(EmbedUtils.embedMessage("Something went wrong when attempting to " +
                                        "download the file. Contact the developers immediately!").build())
                                .queue();
                        return;
                    }
                }
                default -> {
                    msg.replyEmbeds(EmbedUtils.embedMessage("Invalid file.").build())
                            .queue();
                }
            }

            return;
        }

        String link = String.join(" ", args);

        if (!GeneralUtils.isUrl(link)) {
            link = "ytsearch:" + link;
        }

        String finalLink = link;
        channel.sendMessageEmbeds(EmbedUtils.embedMessage("Adding to queue...").build()).queue(addingMsg -> {
            RobertifyAudioManager.getInstance()
                    .loadAndPlay(channel, finalLink, selfVoiceState, memberVoiceState, ctx, addingMsg);
        });
    }

    @Override
    public String getName() {
        return "play";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`\n" +
                "Plays a song\n\n" +
                "**__Usages__**\n" +
                "`" + prefix + "play <song>`\n" +
                "`"+ prefix +"play file` *(Must have a file attached to the message)*";
    }

    @Override
    public List<String> getAliases() {
        return List.of("p", "rob");
    }
}
