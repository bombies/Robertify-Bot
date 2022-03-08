package main.commands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.RobertifyTheme;
import main.constants.Toggles;
import main.constants.TrackSource;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.legacy.InteractiveCommand;
import main.utils.component.interactions.selectionmenu.SelectionMenuOption;
import main.utils.database.mongodb.cache.FavouriteTracksCache;
import main.utils.json.restrictedchannels.RestrictedChannelsConfig;
import main.utils.json.themes.ThemesConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.pagination.Pages;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;

public class FavouriteTracksCommand extends InteractiveCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var args = ctx.getArgs();
        final var member=  ctx.getMember();
        final var msg = ctx.getMessage();
        final var channel = ctx.getChannel();

        if (args.isEmpty()) {
            handeList(channel, member);
        } else {
            switch (args.get(0).toLowerCase()) {
                case "add" -> msg.replyEmbeds(handleAdd(ctx.getGuild(), member)).queue();
                case "remove" -> {
                    final var guild = msg.getGuild();

                    if (args.size() < 2) {
                        msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide the ID of your favourite track to remove!").build())
                                .queue();
                        return;
                    }

                    if (!GeneralUtils.stringIsInt(args.get(1))) {
                        msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid integer as an ID!").build())
                                .queue();
                        return;
                    }

                    final int id = Integer.parseInt(args.get(1));
                    msg.replyEmbeds(handleRemove(ctx.getGuild(), ctx.getAuthor(), id)).queue();
                }
                case "clear" -> msg.replyEmbeds(handleClear(ctx.getGuild(), ctx.getAuthor())).queue();
                case "list" -> handeList(channel, member);
            }
        }
    }

    public MessageEmbed handleAdd(Guild guild, @NotNull Member member) {
        final var config = FavouriteTracksCache.getInstance();
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();
        final var playingTrack = audioPlayer.getPlayingTrack();
        final var memberVoiceState = member.getVoiceState();

        if (!memberVoiceState.inVoiceChannel())
            return RobertifyEmbedUtils.embedMessage(guild, "You must be in a voice channel to use this command!").build();

        final var selfVoiceState = guild.getSelfMember().getVoiceState();

        if (!selfVoiceState.inVoiceChannel())
            return RobertifyEmbedUtils.embedMessage(guild, "I must be playing music in order for this command to work!").build();

        if (!selfVoiceState.getChannel().equals(memberVoiceState.getChannel()))
            return RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!\n\n" +
                    "I am currently in: " + selfVoiceState.getChannel().getAsMention()).build();

        if (playingTrack == null)
            return RobertifyEmbedUtils.embedMessage(guild, "I must be playing music in order for this command to work!").build();

        final var trackInfo = playingTrack.getInfo();

        String id = trackInfo.getIdentifier();
        TrackSource source;

        switch (playingTrack.getInfo().getSourceName()) {
            case "spotify" -> source = TrackSource.SPOTIFY;
            case "deezer" -> source = TrackSource.DEEZER;
            case "youtube" -> source = TrackSource.YOUTUBE;
            default -> {
                return RobertifyEmbedUtils.embedMessage(guild, "The track from this source cannot be added as a favourite track!").build();
            }
        }

        try {
            config.addTrack(member.getIdLong(), id, trackInfo.getTitle(), trackInfo.getAuthor(), source);
            return RobertifyEmbedUtils.embedMessage(guild, "You have added `"+playingTrack.getInfo().getTitle()+" - "+ playingTrack.getInfo().getAuthor() +"` as a favourite track!").build();
        } catch (IllegalArgumentException e) {
            return RobertifyEmbedUtils.embedMessage(guild, e.getMessage()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return RobertifyEmbedUtils.embedMessage(guild, "An unexpected error occurred!" +
                    "\nPlease run the `support` command to join our support server and query the issue.").build();
        }
    }

    private MessageEmbed handleRemove(Guild guild, User user, int id) {
        if (id <= 0)
            return RobertifyEmbedUtils.embedMessage(guild, "The ID **must** be greater than zero!").build();

        final var config = FavouriteTracksCache.getInstance();
        final var trackList = config.getTracks(user.getIdLong());

        if (id > trackList.size())
            return RobertifyEmbedUtils.embedMessage(guild, "That ID is out of bounds!").build();

        try {
            config.removeTrack(user.getIdLong(), id-1);
            final var trackRemoved = trackList.get(id-1);
            return RobertifyEmbedUtils.embedMessage(guild, "You have removed `"+trackRemoved.title()+" by "+trackRemoved.author()+"` as a favourite track!").build();
        } catch (NullPointerException e) {
            return RobertifyEmbedUtils.embedMessage(guild, "You do not have any favourite tracks!").build();
        } catch (Exception e) {
            e.printStackTrace();
            return RobertifyEmbedUtils.embedMessage(guild, "An unexpected error occurred!" +
                    "\nPlease run the `support` command to join our support server and query the issue.").build();
        }
    }

    private MessageEmbed handleClear(Guild guild, User user) {
        final var config = FavouriteTracksCache.getInstance();
        final var trackList = config.getTracks(user.getIdLong());

        if (trackList.isEmpty())
            return RobertifyEmbedUtils.embedMessage(guild, "You do not have any tracks to clear!").build();

        try {
            config.clearTracks(user.getIdLong());
            return RobertifyEmbedUtils.embedMessage(guild, "You have cleared all your favourite tracks!").build();
        } catch (NullPointerException e) {
            return RobertifyEmbedUtils.embedMessage(guild, "You do not have any favourite tracks!").build();
        } catch (Exception e) {
            e.printStackTrace();
            return RobertifyEmbedUtils.embedMessage(guild, "An unexpected error occurred!" +
                    "\nPlease run the `support` command to join our support server and query the issue.").build();
        }
    }

    private void handeList(TextChannel channel, Member member) {
        final var config = FavouriteTracksCache.getInstance();
        final var guild = member.getGuild();

        final var tracks = config.getTracks(member.getIdLong());

        if (tracks.isEmpty()) {
            channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have no favourite tracks").build())
                    .queue();
            return;
        }

        final List<SelectionMenuOption> list = new ArrayList<>();

        for (final var track : tracks)
            list.add(
                    SelectionMenuOption.of(
                            track.title().substring(0, Math.min(75, track.title().length()))
                                    + " by "
                                    + track.author().substring(0, Math.min(20, track.author().length())),
                            "favouriteTrack:" + track.id() + ":" + track.source())
            );

        final var theme = new ThemesConfig().getTheme(guild.getIdLong());
        setDefaultEmbed(member, tracks, theme);
        Pages.paginateMenu(channel, member.getUser(),  list, 0,true);
    }

    private void handeSlashList(SlashCommandEvent event) {
        final var config = FavouriteTracksCache.getInstance();
        final var member = event.getMember();
        final var guild = member.getGuild();

        final var tracks = config.getTracks(member.getIdLong());

        if (tracks.isEmpty()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have no favourite tracks").build())
                    .queue();
            return;
        }

        final List<SelectionMenuOption> list = new ArrayList<>();

        for (final var track : tracks)
            list.add(
                    SelectionMenuOption.of(
                            track.title().substring(0, Math.min(75, track.title().length()))
                                    + " by "
                                    + track.author().substring(0, Math.min(20, track.author().length())),
                            "favouriteTrack:" + track.id() + ":" + track.source())
            );

        final var theme = new ThemesConfig().getTheme(guild.getIdLong());
        setDefaultEmbed(member, tracks, theme);
        Pages.paginateMenu(event, list, 0,true);
    }

    @Override
    public String getName() {
        return "favouritetracks";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`\n\n" +
                "This command allows you to interact with tracks you may really like when" +
                " using the bot! Want to save some really good songs for later? No problem! " +
                "We'll store it for you.\n\n" + getUsages(prefix);
    }

    @Override
    public String getUsages(String prefix) {
        return "**__Usages__**\n" +
                "`" + prefix + "favouritetracks add` *(Add the current song as a favourite track)*\n" +
                "`" + prefix + "favouritetracks remove <id>` *(Remove a specified song as a favourite track)*\n" +
                "`" + prefix + "favouritetracks` *(View all your favourite tracks)*\n";
    }

    @Override
    public List<String> getAliases() {
        return List.of("favs", "fav", "favoritetracks", "favtracks");
    }

    @Override
    public void initCommand() {
        setInteractionCommand(getCommand());
        upsertCommand();
    }

    @Override
    public void initCommand(Guild g) {
        setInteractionCommand(getCommand());
        upsertCommand(g);
    }

    private InteractionCommand getCommand() {
        return InteractionCommand.create()
                .setCommand(
                        Command.of(
                                getName(),
                                "Interact with your favourite tracks using this command!",
                                List.of(),
                                List.of(
                                        SubCommand.of(
                                                "view",
                                                "View all of your favourite tracks!"
                                        ),
                                        SubCommand.of(
                                                "add",
                                                "Add the current playing song as one of your favourites!"
                                        ),
                                        SubCommand.of(
                                                "remove",
                                                "Remove a specific track as a favourite track",
                                                List.of(
                                                        CommandOption.of(
                                                                OptionType.INTEGER,
                                                                "id",
                                                                "The ID of the song to remove",
                                                                true
                                                        )
                                                )
                                        ),
                                        SubCommand.of(
                                                "clear",
                                                "Clear all of your favourite tracks!"
                                        )
                                )
                        )
                )
                .build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(getName())) return;

        switch (event.getSubcommandName()) {
            case "view" -> handeSlashList(event);
            case "add" -> event.replyEmbeds(handleAdd(event.getGuild(), event.getMember())).setEphemeral(true).queue();
            case "remove" -> {
                int id = (int)event.getOption("id").getAsLong();
                event.replyEmbeds(handleRemove(event.getGuild(), event.getUser(), id))
                        .setEphemeral(true)
                        .queue();
            }
            case "clear" -> event.replyEmbeds(handleClear(event.getGuild(), event.getUser())).setEphemeral(true).queue();
        }
    }

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        if (!event.getUser().getId().equals(event.getComponentId().split(":")[1]))
            return;

        final var selectionOption = event.getSelectedOptions().get(0).getValue();

        if (!selectionOption.startsWith("favouriteTrack")) return;

        final String id = selectionOption.split(":")[1];
        final TrackSource source = TrackSource.parse(selectionOption.split(":")[2]);
        final var audioManager = RobertifyAudioManager.getInstance();
        final var guild = event.getGuild();
        final var memberVoiceState = event.getMember().getVoiceState();
        final var selfVoiceState = event.getGuild().getSelfMember().getVoiceState();

        if (!memberVoiceState.inVoiceChannel()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You need to be in a voice channel for this to work").build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (selfVoiceState.inVoiceChannel() && !memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfVoiceState.getChannel().getAsMention())
                            .build())
                    .setEphemeral(true)
                    .queue();
            return;
        } else if (!selfVoiceState.inVoiceChannel()) {
            if (new TogglesConfig().getToggle(guild, Toggles.RESTRICTED_VOICE_CHANNELS)) {
                final var restrictedChannelsConfig = new RestrictedChannelsConfig();
                if (!restrictedChannelsConfig.isRestrictedChannel(guild.getIdLong(), memberVoiceState.getChannel().getIdLong(), RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL)) {
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "I can't join this channel!" +
                                    (!restrictedChannelsConfig.getRestrictedChannels(
                                            guild.getIdLong(),
                                            RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL
                                    ).isEmpty()
                                            ?
                                            "\n\nI am restricted to only join\n" + restrictedChannelsConfig.restrictedChannelsToString(
                                                    guild.getIdLong(),
                                                    RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL
                                            )
                                            :
                                            "\n\nRestricted voice channels have been toggled **ON**, but there aren't any set!"
                                    )
                            ).build())
                            .setEphemeral(true)
                            .queue();
                    return;
                }
            }
        }

        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Adding that track to the queue!").build())
                .setEphemeral(true)
                .queue();

        event.getTextChannel().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Adding to queue...").build()).queue(addingMsg -> {
            switch (source) {
                case DEEZER -> audioManager.loadAndPlay("https://www.deezer.com/us/track/" + id, selfVoiceState, memberVoiceState, event.getTextChannel(), event.getUser(), addingMsg, false);
                case SPOTIFY -> audioManager.loadAndPlay("https://open.spotify.com/track/" + id, selfVoiceState, memberVoiceState, event.getTextChannel(), event.getUser(), addingMsg, false);
                case YOUTUBE -> audioManager.loadAndPlay(id, selfVoiceState, memberVoiceState, event.getTextChannel(), event.getUser(), addingMsg, false);
            }
        });
    }

    public static void setDefaultEmbed(Member member, List<FavouriteTracksCache.Track> tracks, RobertifyTheme theme) {
        Pages.setEmbedStyle(
                () -> new EmbedBuilder()
                        .setColor(theme.getColor())
                        .setTitle("⭐ " +  member.getEffectiveName() + "'s Favourite Tracks")
                        .setFooter("Select the song from the options below to add it to the queue. | " + tracks.size() + " tracks")
                        .setDescription("*The tracks on this page are*\n\n")
                        .setThumbnail(member.getEffectiveAvatarUrl())
        );
    }
}
