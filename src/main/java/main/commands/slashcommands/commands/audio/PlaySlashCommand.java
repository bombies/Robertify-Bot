package main.commands.slashcommands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.audio.PlayCommand;
import main.main.Listener;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
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

public class PlaySlashCommand extends AbstractSlashCommand {

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName(new PlayCommand().getName())
                        .setDescription("Play a song! Links are accepted by Spotify, YouTube, SoundCloud, etc...")
                        .addSubCommands(
                                SubCommand.of(
                                        "tracks",
                                        "Play a song! Links are accepted by Spotify, YouTube, SoundCloud, etc...",
                                        List.of(
                                                CommandOption.of(
                                                        OptionType.STRING,
                                                        "tracks",
                                                        "The name/url of the track/album/playlist to play",
                                                        true
                                                )
                                        )
                                ),
                                SubCommand.of(
                                        "nexttracks",
                                        "Add songs to the beginning of the queue! Links are accepted by Spotify, YouTube, SoundCloud, etc...",
                                        List.of(
                                                CommandOption.of(
                                                        OptionType.STRING,
                                                        "tracks",
                                                        "The name/url of the track/album/playlist to play",
                                                        true
                                                )
                                        )
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
        sendRandomMessage(event);

        event.deferReply().queue();

        final var guild = event.getGuild();

        EmbedBuilder eb;
        final TextChannel channel = event.getTextChannel();

        if (!new GuildConfig().announcementChannelIsSet(guild.getIdLong())) {
            if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong())) {
                if (channel.getIdLong() == new DedicatedChannelConfig().getChannelID(guild.getIdLong())) {
                    event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You cannot run this command in this channel " +
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
            eb = RobertifyEmbedUtils.embedMessage(guild, "You need to be in a voice channel for this to work");
            event.getHook().sendMessageEmbeds(eb.build()).setEphemeral(true).queue();
            return;
        }

        if (selfVoiceState.inVoiceChannel() && !memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfVoiceState.getChannel().getAsMention())
                            .build())
                    .queue();
            return;
        }

        switch (event.getSubcommandName()) {
            case "tracks" -> {
                String link = event.getOption("tracks").getAsString();
                if (!GeneralUtils.isUrl(link))
                    link = "ytsearch:" + link;

                handlePlayTracks(event, guild, member, link, false);
            }
            case "nexttracks" -> {
                String link = event.getOption("tracks").getAsString();
                if (!GeneralUtils.isUrl(link))
                    link = "ytsearch:" + link;

                handlePlayTracks(event, guild, member, link, true);
            }
        }
    }

    private void handlePlayTracks(SlashCommandEvent event, Guild guild, Member member, String link, boolean addToBeginning) {
        event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Adding to queue...").build())
                .setEphemeral(false)
                .queue(msg -> RobertifyAudioManager.getInstance()
                        .loadAndPlay(
                                link,
                                guild.getSelfMember().getVoiceState(),
                                member.getVoiceState(),
                                msg,
                                event,
                                addToBeginning
                        ));
    }
}