package main.commands.slashcommands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.RobertifyTheme;
import main.constants.Toggles;
import main.constants.TrackSource;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.component.interactions.selectionmenu.SelectMenuOption;
import main.utils.database.mongodb.cache.FavouriteTracksCache;
import main.utils.json.restrictedchannels.RestrictedChannelsConfig;
import main.utils.json.themes.ThemesConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import main.utils.pagination.Pages;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;

public class FavouriteTracksCommand extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var args = ctx.getArgs();
        final var member=  ctx.getMember();
        final var msg = ctx.getMessage();
        final var channel = ctx.getChannel();
        final var localeManager = LocaleManager.getLocaleManager(msg.getGuild());

        if (args.isEmpty()) {
            handeList(channel, member);
        } else {
            switch (args.get(0).toLowerCase()) {
                case "add" -> msg.replyEmbeds(handleAdd(ctx.getGuild(), member)).queue();
                case "remove" -> {
                    final var guild = msg.getGuild();

                    if (args.size() < 2) {
                        msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.FavouriteTracksMessages.FT_INVALID_ID)).build())
                                .queue();
                        return;
                    }

                    if (!GeneralUtils.stringIsInt(args.get(1))) {
                        msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.FavouriteTracksMessages.FT_INVALID_SOURCE)).build())
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

    @Override
    public boolean isPremiumCommand() {
        return true;
    }

    public MessageEmbed handleAdd(Guild guild, @NotNull Member member) {
        final var config = FavouriteTracksCache.getInstance();
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();
        final var playingTrack = audioPlayer.getPlayingTrack();
        final var memberVoiceState = member.getVoiceState();

        if (!memberVoiceState.inAudioChannel())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL).build();

        final var selfVoiceState = guild.getSelfMember().getVoiceState();

        if (!selfVoiceState.inAudioChannel())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.MUST_BE_PLAYING).build();

        if (!selfVoiceState.getChannel().equals(memberVoiceState.getChannel()))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention())).build();

        if (playingTrack == null)
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.MUST_BE_PLAYING).build();

        final var trackInfo = playingTrack.getInfo();

        String id = trackInfo.identifier;
        TrackSource source;

        switch (playingTrack.getSourceManager().getSourceName()) {
            case "spotify" -> source = TrackSource.SPOTIFY;
            case "deezer" -> source = TrackSource.DEEZER;
            case "youtube" -> source = TrackSource.YOUTUBE;
            default -> {
                return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.FT_INVALID_SOURCE).build();
            }
        }

        try {
            config.addTrack(member.getIdLong(), id, trackInfo.title, trackInfo.author, source);
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.FAV_TRACK_ADDED, Pair.of("{title}", playingTrack.getInfo().title), Pair.of("{author}", playingTrack.getInfo().author)).build();
        } catch (IllegalArgumentException e) {
            return RobertifyEmbedUtils.embedMessage(guild, e.getMessage()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.UNEXPECTED_ERROR).build();
        }
    }

    private MessageEmbed handleRemove(Guild guild, User user, int id) {

        if (id <= 0)
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.ID_GT_ZERO).build();

        final var config = FavouriteTracksCache.getInstance();
        final var trackList = config.getTracks(user.getIdLong());

        if (id > trackList.size())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.ID_OUT_OF_BOUNDS).build();

        try {
            config.removeTrack(user.getIdLong(), id-1);
            final var trackRemoved = trackList.get(id-1);
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.FAV_TRACK_REMOVED, Pair.of("{title}", trackRemoved.title()), Pair.of("{author}", trackRemoved.author())).build();
        } catch (NullPointerException e) {
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.NO_FAV_TRACKS).build();
        } catch (Exception e) {
            e.printStackTrace();
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.UNEXPECTED_ERROR).build();
        }
    }

    private MessageEmbed handleClear(Guild guild, User user) {
        final var config = FavouriteTracksCache.getInstance();
        final var trackList = config.getTracks(user.getIdLong());

        if (trackList.isEmpty())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.NO_FAV_TRACKS).build();

        try {
            config.clearTracks(user.getIdLong());
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.FAV_TRACKS_CLEARED).build();
        } catch (NullPointerException e) {
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.NO_FAV_TRACKS).build();
        } catch (Exception e) {
            e.printStackTrace();
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.UNEXPECTED_ERROR).build();
        }
    }

    private void handeList(TextChannel channel, Member member) {
        final var config = FavouriteTracksCache.getInstance();
        final var guild = member.getGuild();

        final var tracks = config.getTracks(member.getIdLong());

        if (tracks.isEmpty()) {
            channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.NO_FAV_TRACKS).build())
                    .queue();
            return;
        }

        final List<SelectMenuOption> list = new ArrayList<>();
        final var localeManager = LocaleManager.getLocaleManager(guild);

        for (final var track : tracks)
            list.add(
                    SelectMenuOption.of(
                            localeManager.getMessage(
                                    RobertifyLocaleMessage.FavouriteTracksMessages.FT_SELECT_MENU_OPTION,
                                    Pair.of("{title}", track.title().substring(0, Math.min(75, track.title().length()))),
                                    Pair.of("{author}", track.author().substring(0, Math.min(20, track.author().length())))
                            ),
                            "favouriteTrack:" + track.id() + ":" + track.source())
            );

        final var theme = new ThemesConfig(guild).getTheme();
        setDefaultEmbed(member, tracks, theme);
        Pages.paginateMenu(channel, member.getUser(),  list, 0,true);
    }

    private void handeSlashList(SlashCommandInteractionEvent event) {
        final var config = FavouriteTracksCache.getInstance();
        final var member = event.getMember();
        final var guild = member.getGuild();

        final var tracks = config.getTracks(member.getIdLong());

        if (tracks.isEmpty()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.NO_FAV_TRACKS).build())
                    .queue();
            return;
        }

        final List<SelectMenuOption> list = new ArrayList<>();
        final var localeManager = LocaleManager.getLocaleManager(guild);

        for (final var track : tracks)
            list.add(
                    SelectMenuOption.of(
                            localeManager.getMessage(
                                    RobertifyLocaleMessage.FavouriteTracksMessages.FT_SELECT_MENU_OPTION,
                                    Pair.of("{title}", track.title().substring(0, Math.min(75, track.title().length()))),
                                    Pair.of("{author}", track.author().substring(0, Math.min(20, track.author().length())))
                            ),
                            "favouriteTrack:" + track.id() + ":" + track.source())
            );

        final var theme = new ThemesConfig(guild).getTheme();
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
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("favouritetracks")
                        .setDescription("Interact with your favourite tracks using this command!")
                        .addSubCommands(
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
                        .setPremium()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "This command allows you to interact with tracks you may really like when" +
                " using the bot! Want to save some really good songs for later? No problem! " +
                "We'll store it for you.\n\n" + getUsages();
    }

    @Override
    public String getUsages() {
        return """
                **__Usages__**
                `/favouritetracks add` *(Add the current song as a favourite track)*
                `/favouritetracks remove <id>` *(Remove a specified song as a favourite track)*
                `/favouritetracks` *(View all your favourite tracks)*
                """;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checksWithPremium(event)) return;
        sendRandomMessage(event);

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
    public void onSelectMenuInteraction(@NotNull SelectMenuInteractionEvent event) {
        if (!event.getSelectMenu().getId().startsWith("menupage")) return;
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

        if (!memberVoiceState.inAudioChannel()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (selfVoiceState.inAudioChannel() && !memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()))
                            .build())
                    .setEphemeral(true)
                    .queue();
            return;
        } else if (!selfVoiceState.inAudioChannel()) {
            if (new TogglesConfig(guild).getToggle(Toggles.RESTRICTED_VOICE_CHANNELS)) {
                final var restrictedChannelsConfig = new RestrictedChannelsConfig(guild);
                final var localeManager = LocaleManager.getLocaleManager(guild);
                if (!restrictedChannelsConfig.isRestrictedChannel(memberVoiceState.getChannel().getIdLong(), RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL)) {
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.CANT_JOIN_CHANNEL) +
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
                            .setEphemeral(true)
                            .queue();
                    return;
                }
            }
        }

        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.FT_ADDING_TO_QUEUE).build())
                .setEphemeral(true)
                .queue();

        event.getChannel().asTextChannel().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.FT_ADDING_TO_QUEUE_2).build()).queue(addingMsg -> {
            switch (source) {
                case DEEZER -> audioManager.loadAndPlay("https://www.deezer.com/us/track/" + id, selfVoiceState, memberVoiceState, event.getChannel().asTextChannel(), event.getUser(), addingMsg, false);
                case SPOTIFY -> audioManager.loadAndPlay("https://open.spotify.com/track/" + id, selfVoiceState, memberVoiceState, event.getChannel().asTextChannel(), event.getUser(), addingMsg, false);
                case YOUTUBE -> audioManager.loadAndPlay(id, selfVoiceState, memberVoiceState, event.getChannel().asTextChannel(), event.getUser(), addingMsg, false);
            }
        });
    }

    public static void setDefaultEmbed(Member member, List<FavouriteTracksCache.Track> tracks, RobertifyTheme theme) {
        final var localeManager  = LocaleManager.getLocaleManager(member.getGuild());
        Pages.setEmbedStyle(
                () -> new EmbedBuilder()
                        .setColor(theme.getColor())
                        .setTitle(localeManager.getMessage(RobertifyLocaleMessage.FavouriteTracksMessages.FT_EMBED_TITLE, Pair.of("{user}", member.getEffectiveName())))
                        .setFooter(localeManager.getMessage(RobertifyLocaleMessage.FavouriteTracksMessages.FT_EMBED_FOOTER, Pair.of("{tracks}", String.valueOf(tracks.size()))))
                        .setDescription(localeManager.getMessage(RobertifyLocaleMessage.FavouriteTracksMessages.FT_EMBED_DESCRIPTION))
                        .setThumbnail(member.getEffectiveAvatarUrl())
        );
    }
}
