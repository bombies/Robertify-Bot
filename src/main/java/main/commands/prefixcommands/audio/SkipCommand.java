package main.commands.prefixcommands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.BotConstants;
import main.constants.Permission;
import main.constants.Toggles;
import main.exceptions.AutoPlayException;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.requestchannel.RequestChannelConfig;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

@Deprecated @ForRemoval
public class SkipCommand extends ListenerAdapter implements ICommand {
    private final static HashMap<Long, Integer> voteSkips = new HashMap<>();
    private final static HashMap<Long, ArrayList<Long>> voters = new HashMap<>();
    private final static HashMap<Long, Pair<Long, Long>> voteSkipMessages = new HashMap<>();
    private final static HashMap<Long, Long> voteSkipStarters = new HashMap<>();

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final Member self = ctx.getSelfMember();
        final GuildVoiceState selfVoiceState = self.getVoiceState();
        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();
        final Guild guild = ctx.getGuild();
        final var togglesConfig = new TogglesConfig(guild);

        if (!togglesConfig.isDJToggleSet(this)) {
            msg.replyEmbeds(handleSkip(selfVoiceState, memberVoiceState)).queue();
            return;
        }

        if (togglesConfig.getDJToggle(this)) {
            if (togglesConfig.getToggle(Toggles.VOTE_SKIPS)) {
                if (!GeneralUtils.hasPerms(guild, member, Permission.ROBERTIFY_DJ)) {
                    if (selfVoiceState.inAudioChannel()) {
                        msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.VOICE_CHANNEL_NEEDED).build())
                                .queue();
                        return;
                    }

                    if (selfVoiceState.getChannel().getMembers().size() != 2) {
                        MessageEmbed embed = handleVoteSkip(ctx.getChannel(), selfVoiceState, memberVoiceState);
                        if (embed != null)
                            msg.replyEmbeds(handleVoteSkip(ctx.getChannel(), selfVoiceState, memberVoiceState)).queue();
                    } else msg.replyEmbeds(handleSkip(selfVoiceState, memberVoiceState)).queue();
                } else msg.replyEmbeds(handleSkip(selfVoiceState, memberVoiceState)).queue();
            } else  {
                if (GeneralUtils.hasPerms(guild, member, Permission.ROBERTIFY_DJ)) {
                    msg.replyEmbeds(handleSkip(selfVoiceState, memberVoiceState)).queue();
                } else msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, BotConstants.getInsufficientPermsMessage(guild, Permission.ROBERTIFY_DJ)).build())
                        .queue();
            }
        } else msg.replyEmbeds(handleSkip(selfVoiceState, memberVoiceState)).queue();
    }

    public MessageEmbed handleSkip(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState) {
        final var guild = selfVoiceState.getGuild();

        if (checks(selfVoiceState, memberVoiceState) != null)
            return checks(selfVoiceState, memberVoiceState);

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();

        if (audioPlayer.getPlayingTrack() == null)
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.SkipMessages.NOTHING_TO_SKIP).build();

        skip(guild);

        new LogUtils(guild).sendLog(LogType.TRACK_SKIP, RobertifyLocaleMessage.SkipMessages.SKIPPED_LOG, Pair.of("{user}", memberVoiceState.getMember().getAsMention()));
        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.SkipMessages.SKIPPED).build();
    }

    public MessageEmbed handleVoteSkip(GuildMessageChannel channel, GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState) {
        final var guild = selfVoiceState.getGuild();

        if (checks(selfVoiceState, memberVoiceState) != null)
            return checks(selfVoiceState, memberVoiceState);

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();

        if (audioPlayer.getPlayingTrack() == null)
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.SkipMessages.NOTHING_TO_SKIP).build();

        final int neededVotes = getNeededVotes(guild);
        channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild,
                        RobertifyLocaleMessage.SkipMessages.VOTE_SKIP_STARTED_EMBED,
                        Pair.of("{user}", memberVoiceState.getMember().getAsMention()),
                        Pair.of("{neededVotes}", String.valueOf(neededVotes))).build()
                )
                .setActionRow(
                        Button.of(ButtonStyle.SUCCESS, "voteskip:upvote:" + guild.getId(), "Vote"),
                        Button.of(ButtonStyle.DANGER, "voteskip:cancel:" + guild.getId(), "Cancel")
                ).queue(success -> {
                    new LogUtils(guild).sendLog(LogType.TRACK_VOTE_SKIP, RobertifyLocaleMessage.SkipMessages.VOTE_SKIP_STARTED_LOG, Pair.of("{user}", memberVoiceState.getMember().getAsMention()));
                    voteSkips.put(guild.getIdLong(), 1);
                    voteSkipMessages.put(guild.getIdLong(), Pair.of(channel.getIdLong(), success.getIdLong()) );

                    ArrayList<Long> list = new ArrayList<>();
                    list.add(memberVoiceState.getIdLong());
                    voters.put(guild.getIdLong(), list);

                    voteSkipStarters.put(guild.getIdLong(), memberVoiceState.getMember().getIdLong());
                });
        return null;
    }

    private void skip(Guild guild) {
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();
        final var scheduler = musicManager.getScheduler();

<<<<<<< HEAD
        final var playingTrack = audioPlayer.getPlayingTrack();
        final var pastQueue = musicManager.getScheduler().getPastQueue();
        pastQueue.push(audioPlayer.getPlayingTrack());
=======
        AudioTrack playingTrack = audioPlayer.getPlayingTrack();
        HashMap<Long, Stack<AudioTrack>> pastQueue = musicManager.getScheduler().getPastQueue();
        if (!pastQueue.containsKey(guild.getIdLong()))
            pastQueue.put(guild.getIdLong(), new Stack<>());
        pastQueue.get(guild.getIdLong()).push(audioPlayer.getPlayingTrack());

        if (scheduler.isRepeating())
            scheduler.setRepeating(false);
>>>>>>> 1f173d748606af2f96273701cdce66240218a441

        if (scheduler.isRepeating())
            scheduler.setRepeating(false);

        try {
            musicManager.getScheduler().nextTrack(playingTrack, true, audioPlayer.getTrackPosition());
        } catch (AutoPlayException e) {
            scheduler.getAnnouncementChannel().sendMessageEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild,
                    "AutoPlay","Could not auto-play because tracks from `"+ playingTrack.getSourceManager().getSourceName()+"` aren't supported!").build())
                    .queue();
            scheduler.scheduleDisconnect(true);
        }

        if (new RequestChannelConfig(guild).isChannelSet())
            new RequestChannelConfig(guild).updateMessage();

        clearVoteSkipInfo(guild);
    }

    public static void clearVoteSkipInfo(Guild guild) {
        voters.remove(guild.getIdLong());
        voteSkips.remove(guild.getIdLong());
        voteSkipStarters.remove(guild.getIdLong());

        if (voteSkipMessages.get(guild.getIdLong()) != null) {
            Pair<Long, Long> pair = voteSkipMessages.get(guild.getIdLong());
            guild.getTextChannelById(pair.getLeft()).retrieveMessageById(pair.getRight()).complete()
                    .editMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.SkipMessages.SKIPPED)
                            .build())
                    .queue();
        }

        voteSkipMessages.remove(guild.getIdLong());
    }

    private MessageEmbed checks(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState) {
        final var guild = selfVoiceState.getGuild();

        if (!selfVoiceState.inAudioChannel())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NOTHING_PLAYING).build();

        if (!memberVoiceState.inAudioChannel())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.USER_VOICE_CHANNEL_NEEDED).build();

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel()))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention())).build();

        return null;
    }

    public boolean voteSkipIsActive(Guild guild) {
        return voteSkips.containsKey(guild.getIdLong());
    }

    private void incrementVoteSkip(Guild guild) {
        if (!voteSkipIsActive(guild))
            throw new IllegalStateException("Can't increment vote skip since guild doesn't have any active vote skips.");

        voteSkips.put(guild.getIdLong(), voteSkips.get(guild.getIdLong()) + 1);

        final int neededVotes = getNeededVotes(guild);
        if (voteSkips.get(guild.getIdLong()) == neededVotes)
            doVoteSkip(guild);
    }

    private void doVoteSkip(Guild guild) {
        Pair<Long, Long> pair = voteSkipMessages.get(guild.getIdLong());
        Message message = guild.getTextChannelById(pair.getLeft())
                .retrieveMessageById(pair.getRight()).complete();

        voteSkipMessages.remove(guild.getIdLong());

        AudioTrackInfo info = RobertifyAudioManager.getInstance()
                .getMusicManager(guild)
                .getPlayer()
                .getPlayingTrack()
                .getInfo();

        skip(guild);
        message.editMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.SkipMessages.VOTE_SKIPPED,
                    Pair.of("{title}", info.title),
                    Pair.of("{author}", info.author)
                ).build())
                .setComponents()
                .queue();

        new LogUtils(guild).sendLog(LogType.TRACK_SKIP, RobertifyLocaleMessage.SkipMessages.VOTE_SKIPPED_LOG,
                Pair.of("{title}", info.title),
                Pair.of("{author}", info.author)
        );
    }

    private void decrementVoteSkip(Guild guild) {
        if (!voteSkipIsActive(guild))
            throw new IllegalStateException("Can't decrement vote skip since guild doesn't have any active vote skips.");

        if (voteSkips.get(guild.getIdLong()) == 0)
            throw new IllegalStateException("Can't decrement vote skip past zero!");

        voteSkips.put(guild.getIdLong(), voteSkips.get(guild.getIdLong()) - 1);
    }

    private void updateVoteSkipMessage(Guild guild) {
        if (!voteSkipIsActive(guild))
            return;

        Pair<Long, Long> pair = voteSkipMessages.get(guild.getIdLong());
        Message message = guild.getTextChannelById(pair.getLeft()).retrieveMessageById(pair.getRight()).complete();

        final int neededVotes = getNeededVotes(guild);
        message.editMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.SkipMessages.VOTE_SKIP_STARTED_EMBED,
                    Pair.of("{user}", GeneralUtils.toMention(guild, voteSkipStarters.get(guild.getIdLong()), GeneralUtils.Mentioner.USER)),
                    Pair.of("{neededVotes}", String.valueOf(neededVotes))
                ).build()).queue();
    }

    private int getNeededVotes(Guild guild) {
        final int size = guild.getSelfMember().getVoiceState().getChannel().getMembers().size();
        return (int)Math.ceil(size * (50/100.0));
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.getButton().getId().startsWith("voteskip:"))
            return;

        String[] split = event.getButton().getId().split(":");
        final String voteType = split[1];
        final String guildID = split[2];

        Guild guild = event.getGuild();
        if (!guild.getId().equals(guildID))
            return;

        GuildVoiceState voiceState = event.getGuild().getSelfMember().getVoiceState();
        GuildVoiceState memberState = event.getMember().getVoiceState();

        if (!voiceState.inAudioChannel()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.BUTTON_NO_LONGER_VALID).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!memberState.inAudioChannel()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_BUTTON).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!voiceState.getChannel().equals(memberState.getChannel())) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_BUTTON).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        User user = event.getUser();
        switch (voteType) {
            case "upvote" -> {
                ArrayList<Long> list = voters.get(guild.getIdLong());
                if (list.contains(user.getIdLong())) {
                    list.remove(user.getIdLong());
                    voters.put(guild.getIdLong(), list);
                    decrementVoteSkip(guild);
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.SkipMessages.SKIP_VOTE_REMOVED).build())
                            .setEphemeral(true)
                            .queue();
                    updateVoteSkipMessage(guild);
                } else {
                    if (GeneralUtils.hasPerms(guild, event.getMember(), Permission.ROBERTIFY_DJ)) {
                        doVoteSkip(guild);
                        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.SkipMessages.DJ_SKIPPED).build())
                                .setEphemeral(true)
                                .queue();
                        return;
                    }

                    list.add(user.getIdLong());
                    voters.put(guild.getIdLong(), list);
                    incrementVoteSkip(guild);
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.SkipMessages.SKIP_VOTE_ADDED).build())
                            .setEphemeral(true)
                            .queue();
                    updateVoteSkipMessage(guild);
                }
            }
            case "cancel" -> {
                if (voteSkipStarters.get(guild.getIdLong()) != user.getIdLong()) {
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessage(
                            guild,
                            RobertifyLocaleMessage.GeneralMessages.NO_PERMS_BUTTON
                    ).build())
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                Pair<Long, Long> pair = voteSkipMessages.get(guild.getIdLong());
                Message message = guild.getTextChannelById(pair.getLeft())
                        .retrieveMessageById(pair.getRight()).complete();

                AudioTrackInfo info = RobertifyAudioManager.getInstance()
                        .getMusicManager(guild)
                        .getPlayer()
                        .getPlayingTrack()
                        .getInfo();

                voteSkipMessages.remove(guild.getIdLong());
                clearVoteSkipInfo(guild);
                message.editMessageEmbeds(RobertifyEmbedUtils.embedMessage(
                        guild,
                        RobertifyLocaleMessage.SkipMessages.VOTE_SKIP_CANCELLED,
                        Pair.of("{title}", info.title),
                        Pair.of("{author}", info.author)
                        ).build()
                ).queue();
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
