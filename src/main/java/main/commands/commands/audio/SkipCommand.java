package main.commands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.Permission;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import main.utils.json.toggles.TogglesConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SkipCommand extends ListenerAdapter implements ICommand {
    private final HashMap<Long, Integer> voteSkips = new HashMap<>();
    private final HashMap<Long, List<Long>> voters = new HashMap<>();
    private final HashMap<Long, Pair<Long, Long>> voteSkipMessages = new HashMap<>();
    private final HashMap<Long, String> voteSkipStarters = new HashMap<>();

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final Member self = ctx.getSelfMember();
        final GuildVoiceState selfVoiceState = self.getVoiceState();
        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();
        final Guild guild = ctx.getGuild();

        if (!new TogglesConfig().isDJToggleSet(guild, this))
            return;

        if (new TogglesConfig().getDJToggle(guild, this)) {
            if (!GeneralUtils.hasPerms(guild, member, Permission.ROBERTIFY_DJ)) {
                if (selfVoiceState.getChannel().getMembers().size() != 1) {
                    MessageEmbed embed = handleVoteSkip(ctx.getChannel(), selfVoiceState, memberVoiceState);
                    if (embed != null)
                        msg.replyEmbeds(handleVoteSkip(ctx.getChannel(), selfVoiceState, memberVoiceState)).queue();
                } else msg.replyEmbeds(handleSkip(selfVoiceState, memberVoiceState)).queue();
            } else msg.replyEmbeds(handleSkip(selfVoiceState, memberVoiceState)).queue();
        } else msg.replyEmbeds(handleSkip(selfVoiceState, memberVoiceState)).queue();
    }

    public MessageEmbed handleSkip(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState) {
        final var guild = selfVoiceState.getGuild();

        if (checks(selfVoiceState, memberVoiceState) != null)
            return checks(selfVoiceState, memberVoiceState);

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();
        final var scheduler = musicManager.getScheduler();

        if (audioPlayer.getPlayingTrack() == null)
            return RobertifyEmbedUtils.embedMessage(guild, "There is nothing to skip!").build();

        musicManager.getScheduler().getPastQueue().push(audioPlayer.getPlayingTrack());

        if (scheduler.repeating)
            scheduler.repeating = false;

        musicManager.getScheduler().nextTrack(audioPlayer.getPlayingTrack(), true, audioPlayer.getTrackPosition());

        if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
            new DedicatedChannelConfig().updateMessage(guild);

        LofiCommand.getLofiEnabledGuilds().remove(guild.getIdLong());

        new LogUtils().sendLog(guild, LogType.TRACK_SKIP, memberVoiceState.getMember().getAsMention() + " has skipped the song");
        return RobertifyEmbedUtils.embedMessage(guild, "Skipped the song!").build();
    }

    public MessageEmbed handleVoteSkip(TextChannel channel, GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState) {
        final var guild = selfVoiceState.getGuild();

        if (checks(selfVoiceState, memberVoiceState) != null)
            return checks(selfVoiceState, memberVoiceState);

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();

        if (audioPlayer.getPlayingTrack() == null)
            return RobertifyEmbedUtils.embedMessage(guild, "There is nothing to skip!").build();

        final int size = selfVoiceState.getChannel().getMembers().size();

        final int neededVotes = (int)Math.ceil(size * (50/100.0));
        channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, memberVoiceState.getMember().getAsMention() + " has started a vote skip!\n\n" +
                        "**Votes: 1/" + neededVotes + "**").build())
                .setActionRow(
                        Button.of(ButtonStyle.SUCCESS, "voteskip:upvote:" + guild.getId(), "Vote")
                ).queue(success -> {
                    new LogUtils().sendLog(guild, LogType.TRACK_VOTE_SKIP, memberVoiceState.getMember().getAsMention() + " has started a vote skip.");
                    voteSkips.put(guild.getIdLong(), 1);
                    voteSkipMessages.put(guild.getIdLong(), Pair.of(channel.getIdLong(), success.getIdLong()) );
                    voters.put(guild.getIdLong(), List.of(memberVoiceState.getIdLong()));
                    voteSkipStarters.put(guild.getIdLong(), memberVoiceState.getMember().getAsMention());
                });
        return null;
    }

    private MessageEmbed checks(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState) {
        final var guild = selfVoiceState.getGuild();

        if (!selfVoiceState.inVoiceChannel())
            return RobertifyEmbedUtils.embedMessage(guild, "There is nothing playing!").build();

        if (!memberVoiceState.inVoiceChannel())
            return RobertifyEmbedUtils.embedMessage(guild, "You need to be in a voice channel for this to work").build();

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel()))
            return RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfVoiceState.getChannel().getAsMention()).build();

        return null;
    }

    public boolean voteSkipIsActive(Guild guild) {
        return voteSkips.containsKey(guild.getIdLong());
    }

    private void incrementVoteSkip(Guild guild) {
        if (!voteSkipIsActive(guild))
            throw new IllegalStateException("Can't increment vote skip since guild doesn't have any active vote skips.");

        voteSkips.put(guild.getIdLong(), voteSkips.get(guild.getIdLong()) + 1);
    }

    private void decrementVoteSkip(Guild guild) {
        if (!voteSkipIsActive(guild))
            throw new IllegalStateException("Can't decrement vote skip since guild doesn't have any active vote skips.");

        if (voteSkips.get(guild.getIdLong()) == 0)
            throw new IllegalStateException("Can't decrement vote skip past zero!");

        voteSkips.put(guild.getIdLong(), voteSkips.get(guild.getIdLong()) + 1);
    }

    private void updateVoteSkipMessage(Guild guild) {
        if (!voteSkipIsActive(guild))
            throw new IllegalArgumentException("That guild doesn't have an ongoing vote skip!");

        Pair<Long, Long> pair = voteSkipMessages.get(guild.getIdLong());
        Message message = guild.getTextChannelById(pair.getLeft()).retrieveMessageById(pair.getRight()).complete();

        final int size = guild.getSelfMember().getVoiceState().getChannel().getMembers().size();

        final int neededVotes = (int)Math.ceil(size * (50/100.0));
        message.editMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, voteSkipStarters.get(guild.getIdLong()) + " has started a vote skip!\n\n" +
                "**Votes: 1/" + neededVotes + "**").build()).queue();
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        if (event.getButton().getId().startsWith("voteskip:"))
            return;

        String[] split = event.getButton().getId().split(":");
        final String voteType = split[1];
        final String guildID = split[2];

        Guild guild = event.getGuild();
        if (!guild.getId().equals(guildID))
            return;

        switch (voteType) {
            case "upvote" -> {
                User user = event.getUser();
                List<Long> list = voters.get(guild.getIdLong());
                if (list.contains(user.getIdLong())) {
                    list.remove(user.getIdLong());
                    voters.put(guild.getIdLong(), list);
                    decrementVoteSkip(guild);
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have removed your vote!").build())
                            .queue();
                } else {
                    list.add(user.getIdLong());
                    voters.put(guild.getIdLong(), list);
                    incrementVoteSkip(guild);
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have added your vote!").build())
                            .queue();
                }
            }
        }
    }

    @Override
    public String getName() {
        return "skip";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+ GeneralUtils.listToString(getAliases()) +"`\n" +
                "Skips a track";
    }

    @Override
    public List<String> getAliases() {
        return List.of("next");
    }
}
