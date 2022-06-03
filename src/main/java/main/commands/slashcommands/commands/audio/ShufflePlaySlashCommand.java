package main.commands.slashcommands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.main.Listener;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

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
        return "Looking to play a playlist but shuffled right off the bat? This command does that for you.";
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checks(event)) return;
        sendRandomMessage(event);

        EmbedBuilder eb;
        final Guild guild = event.getGuild();

        final Member member = event.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();

        if (!memberVoiceState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.USER_VOICE_CHANNEL_NEEDED);
            event.replyEmbeds(eb.build()).setEphemeral(true).queue();
            return;
        }

        if (selfVoiceState.inVoiceChannel() && !memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()))
                            .build())
                    .queue();
            return;
        }

        String url = event.getOption("playlist").getAsString();

        if (!url.contains("deezer.page.link")) {
            if (url.contains("soundcloud.com") && !url.contains("sets")) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ShufflePlayMessages.NOT_PLAYLIST, Pair.of("{source}", "SoundCloud")).build()).queue();
                return;
            } else if (url.contains("youtube.com") && !url.contains("playlist") && !url.contains("list")) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ShufflePlayMessages.NOT_PLAYLIST, Pair.of("{source}", "YouTube")).build()).queue();
                return;
            } else if (!url.contains("playlist") && !url.contains("album") && !url.contains("soundcloud.com") && !url.contains("youtube.com")) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ShufflePlayMessages.MUST_PROVIDE_VALID_PLAYLIST).build()).queue();
                return;
            }
        }

        event.deferReply().queue();

        event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), RobertifyLocaleMessage.FavouriteTracksMessages.FT_ADDING_TO_QUEUE_2).build()).queue(addingMsg -> {
            RobertifyAudioManager.getInstance()
                    .loadAndPlayShuffled(url, selfVoiceState, memberVoiceState, addingMsg, event, false);
        });
    }
}
