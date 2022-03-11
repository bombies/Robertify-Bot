package main.commands.slashcommands;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.commands.audio.ShufflePlayCommand;
import main.main.Listener;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.component.legacy.InteractiveCommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ShufflePlaySlashCommand extends AbstractSlashCommand {

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("shuffleplay")
                        .setDescription("Play a playlist/album shuffled right off the bat!")
                        .addOptions(
                                CommandOption.of(
                                        OptionType.STRING,
                                        "playlist",
                                        "The playlist/album to play",
                                        true
                                )
                        )
                        .setPossibleDJCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checks(event)) return;

        if (!musicCommandDJCheck(event)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "You need to be a DJ to run this command!").build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        EmbedBuilder eb;
        final Guild guild = event.getGuild();
        final TextChannel channel = event.getTextChannel();

        if (!new GuildConfig().announcementChannelIsSet(guild.getIdLong())) {
            if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong())) {
                if (channel.getIdLong() == new DedicatedChannelConfig().getChannelID(guild.getIdLong())) {
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "You cannot run this command in this channel " +
                                    "without first having an announcement channel set!").build())
                            .setEphemeral(false)
                            .queue();
                    return;
                }
            }
        }

        Listener.checkIfAnnouncementChannelIsSet(guild, channel);

        final Member member = event.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();

        if (!memberVoiceState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(event.getGuild(), "You need to be in a voice channel for this to work");
            event.replyEmbeds(eb.build()).setEphemeral(true).queue();
            return;
        }

        if (selfVoiceState.inVoiceChannel() && !memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfVoiceState.getChannel().getAsMention())
                            .build())
                    .queue();
            return;
        }

        String url = event.getOption("playlist").getAsString();

        if (!url.contains("deezer.page.link")) {
            if (url.contains("soundcloud.com") && !url.contains("sets")) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "This SoundCloud URL doesn't contain a playlist!").build()).queue();
                return;
            } else if (url.contains("youtube.com") && !url.contains("playlist") && !url.contains("list")) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "This YouTube URL doesn't contain a playlist!").build()).queue();
                return;
            } else if (!url.contains("playlist") && !url.contains("album") && !url.contains("soundcloud.com") && !url.contains("youtube.com")) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "You must provide the link of a valid album/playlist!").build()).queue();
                return;
            }
        }

        event.deferReply().queue();

        event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "Adding to queue...").build()).queue(addingMsg -> {
            RobertifyAudioManager.getInstance()
                    .loadAndPlayShuffled(url, selfVoiceState, memberVoiceState, addingMsg, event, false);
        });
    }
}
