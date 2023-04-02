package main.utils.pagination;

import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;
import lombok.SneakyThrows;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.audio.QueueCommand;
import main.constants.InteractionLimits;
import main.utils.RobertifyEmbedUtils;
import main.utils.apis.robertify.imagebuilders.QueueImageBuilder;
import main.utils.component.interactions.selectionmenu.StringSelectMenuOption;
import main.utils.component.interactions.selectionmenu.StringSelectionMenuBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import javax.annotation.Nullable;
import java.io.File;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public abstract class Pages {
    private static final HashMap<Long, List<MessagePage>> messages = new HashMap<>();
    private static final HashMap<Long, List<QueuePage>> queueMessages = new HashMap<>();
    private static final HashMap<Long, List<MenuPage>> menuMessages = new HashMap<>();
    private static Supplier<EmbedBuilder> embedStyle = EmbedBuilder::new;

    private static final Paginator paginator = Paginator.getDefaultPaginator();

    public static Message paginateMessage(GuildMessageChannel channel, User user, List<MessagePage> messagePages) {
        AtomicReference<Message> ret = new AtomicReference<>();

        channel.sendMessageEmbeds(messagePages.get(0).getEmbed()).queue(msg -> {
            if (messagePages.size() > 1) {
                msg.editMessageComponents(
                        Paginator.getButtons(user, false, false, true, true)
                ).queue();

                messages.put(msg.getIdLong(), messagePages);
                ret.set(msg);
            }
        });

        return ret.get();
    }

    public static Message paginateMessage(SlashCommandInteractionEvent event, List<MessagePage> messagePages) {
        AtomicReference<Message> ret = new AtomicReference<>();

        WebhookMessageCreateAction<Message> messageAction = event.getHook().sendMessageEmbeds(messagePages.get(0).getEmbed()).setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()));

        if (messagePages.size() > 1) {
            messageAction = messageAction.addComponents(
                    Paginator.getButtons(event.getUser(), false, false, true, true)
            );
        }

        messageAction.queue(msg -> {
            if (messagePages.size() > 1) {
                messages.put(msg.getIdLong(), messagePages);
                ret.set(msg);
            }
        });

        return ret.get();
    }

    public static Message paginateQueueMessage(SlashCommandInteractionEvent event, List<QueuePage> queuePages) {
        AtomicReference<Message> ret = new AtomicReference<>();
        assert event.getGuild() != null;

        try {
            final var image = queuePages.get(0).getImage();
            WebhookMessageCreateAction<Message> messageAction = event.getHook()
                    .sendFiles(FileUpload.fromData(image))
                    .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()));

            if (queuePages.size() > 1) {
                messageAction = messageAction.addComponents(
                        Paginator.getQueueButtons(event.getUser(), false, false, true, true)
                );
            }

            messageAction.queue(msg -> {
                image.delete();

                if (queuePages.size() > 1) {
                    queueMessages.put(msg.getIdLong(), queuePages);
                    ret.set(msg);
                }
            });
        } catch (SocketTimeoutException | ConnectException e) {
            final var pages = new ArrayList<MessagePage>();
            final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
            final var queue = musicManager.getScheduler().getQueue();
            final var content = new QueueCommand().getContent(event.getGuild(), queue);

            messageLogic(event.getGuild(), pages, content, 10);
            paginateMessage(event, pages);
        }


        return ret.get();
    }

    public static Message paginateMessage(GuildMessageChannel channel, User user, List<String> content, int maxPerPage) {
        List<MessagePage> messagePages = new ArrayList<>();

        messageLogic(channel.getGuild(), messagePages, content, maxPerPage);

        return paginateMessage(channel, user, messagePages);
    }

    public static Message paginateMessage(List<String> content, int maxPerPage, SlashCommandInteractionEvent event) {
        List<MessagePage> messagePages = new ArrayList<>();

        event.deferReply().queue();

        messageLogic(event.getGuild(), messagePages, content, maxPerPage);

        return paginateMessage(event, messagePages);
    }

    public static Message paginateQueue(int maxPerPage, SlashCommandInteractionEvent event) {
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
        final var queue = musicManager.getScheduler().getQueue();
        final var queuePages = new ArrayList<QueuePage>();

        event.deferReply().queue();

        messageLogic(queuePages, queue, maxPerPage);

        return paginateQueueMessage(event, queuePages);
    }

    private static class QueueItem {
        @Getter
        private final int trackIndex;
        @Getter
        private final String trackTitle;
        @Getter
        private final String artist;
        @Getter
        private final long duration;
        @Getter
        @Nullable
        private final String artworkUrl;

        public QueueItem(int trackIndex, String trackTitle, String artist, long duration, String artworkUrl) {
            this.trackIndex = trackIndex;
            this.trackTitle = trackTitle;
            this.artist = artist;
            this.duration = duration;
            this.artworkUrl = artworkUrl;
        }
    }

    protected final static class QueuePage {
        @Getter
        private final int pageNumber;
        @Getter
        private final List<QueueItem> queueItems;

        public QueuePage(int pageNumber, List<QueueItem> queueItems) {
            this.pageNumber = pageNumber;
            this.queueItems = queueItems;
        }

        public QueuePage(int pageNumber) {
            this.pageNumber = pageNumber;
            this.queueItems = new ArrayList<>();
        }

        public void addItem(QueueItem item) {
            queueItems.add(item);
        }

        public void addItem(int trackIndex, String title, String artist, long duration, @Nullable String artworkUrl) {
            queueItems.add(new QueueItem(trackIndex, title, artist, duration, artworkUrl));
        }

        public File getImage() throws SocketTimeoutException, ConnectException {
            final var builder = new QueueImageBuilder()
                    .setPage(pageNumber);

            for (final var item : queueItems)
                builder.addTrack(item.trackIndex, item.trackTitle, item.artist, item.duration, item.artworkUrl);

            return builder.build();
        }
    }

    private static void messageLogic(Guild guild, List<MessagePage> messagePages, List<String> content, int maxPerPage) {
        if (content.size() <= maxPerPage) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "\t");
            for (String str : content)
                eb.appendDescription(str + "\n");
            messagePages.add(new MessagePage(eb.build()));
        } else {
            int pagesRequired = (int) Math.ceil((double) content.size() / maxPerPage);

            int lastIndex = 0;
            for (int i = 0; i < pagesRequired; i++) {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "\t");
                for (int j = 0; j < maxPerPage; j++) {
                    if (lastIndex == content.size()) break;

                    eb.appendDescription(content.get(lastIndex) + "\n");
                    lastIndex++;
                }
                messagePages.add(new MessagePage(eb.build()));
            }
        }
    }

    private static void messageLogic(List<QueuePage> messagePages, ConcurrentLinkedQueue<AudioTrack> queue, int maxPerPage) {
        if (queue.size() <= maxPerPage) {
            final List<QueueItem> items = new ArrayList<>();
            for (int i = 0; i < queue.size(); i++) {
                final var track = queue.stream().toList().get(i);
                final var trackInfo = track.getInfo();
                items.add(new QueueItem(i + 1, trackInfo.title, trackInfo.author, trackInfo.length, track instanceof MirroringAudioTrack mt ? mt.getArtworkURL() : null));
            }
            messagePages.add(new QueuePage(1, items));
        } else {
            final var trackList = queue.stream().toList();
            int pagesRequired = (int) Math.ceil((double) queue.size() / maxPerPage);
            int lastIndex = 0;

            for (int i = 0; i < pagesRequired; i++) {
                final var page = new QueuePage(i + 1);
                for (int j = 0; j < maxPerPage; j++) {
                    if (lastIndex == queue.size()) break;
                    final var track = trackList.get(lastIndex);
                    final var trackInfo = track.getInfo();

                    page.addItem(lastIndex + 1, trackInfo.title, trackInfo.author, trackInfo.length, track instanceof MirroringAudioTrack mt ? mt.getArtworkURL() : null);
                    lastIndex++;
                }
                messagePages.add(page);
            }
        }
    }

    public static List<MessagePage> getMessagePages(long msg) {
        return messages.get(msg);
    }

    public static List<QueuePage> getQueuePages(long msg) {
        return queueMessages.get(msg);
    }

    @SneakyThrows
    public static void paginateMenu(User user, Message msg, List<StringSelectMenuOption> options) {
        List<MenuPage> menuPages = menuLogic(msg.getId(), options);

        final var firstPage = menuPages.get(0);

        SelectMenu menu = StringSelectionMenuBuilder.of(
                "menupage:" + user.getId(),
                "Select an option",
                Pair.of(1, 1),
                firstPage.getOptions().subList(0, Math.min(options.size(), InteractionLimits.SELECTION_MENU))
        ).build();

        msg.editMessageComponents(ActionRow.of(menu))
                .queue(success -> menuMessages.put(msg.getIdLong(), menuPages));
    }

    @SneakyThrows
    public static void paginateMenu(User user, GuildMessageChannel channel, ReplyCallbackAction msg, List<StringSelectMenuOption> options) {
        List<MenuPage> menuPages = menuLogic("null", options);

        final var firstPage = menuPages.get(0);

        SelectMenu menu = StringSelectionMenuBuilder.of(
                "menupage:" + user.getId(),
                "Select an option",
                Pair.of(1, 1),
                firstPage.getOptions().subList(0, Math.min(options.size(), InteractionLimits.SELECTION_MENU))
        ).build();

        msg.addActionRow(menu)
                .setEphemeral(RobertifyEmbedUtils.getEphemeralState(channel))
                .queue(success -> success.retrieveOriginal().queue(og -> menuMessages.put(og.getIdLong(), menuPages)));
    }

    public static void paginateMenu(GuildMessageChannel channel, User user, List<StringSelectMenuOption> options, int startingPage) {
        paginateMenu(channel, user, options, startingPage, false);
    }

    public static void paginateMenu(GuildMessageChannel channel, User user, List<StringSelectMenuOption> options, int startingPage, boolean numberEachEntry) {
        Message msg = menuLogic(channel, options, startingPage, numberEachEntry);
        paginateMenu(user, msg, options);
    }

    public static void paginateMenu(SlashCommandInteractionEvent event, List<StringSelectMenuOption> options, int startingPage, boolean numberEachEntry) {
        final var msg = menuLogic(event, options, startingPage, numberEachEntry);
        paginateMenu(event.getUser(), event.getChannel().asGuildMessageChannel(), msg, options);
    }

    private static Message menuLogic(GuildMessageChannel channel, int startingPage, List<StringSelectMenuOption> options) {
        return menuLogic(channel, options, startingPage, false);
    }

    private static ReplyCallbackAction menuLogic(SlashCommandInteractionEvent event, int startingPage, List<StringSelectMenuOption> options) {
        return menuLogic(event, options, startingPage, false);
    }

    private static Message menuLogic(GuildMessageChannel channel, List<StringSelectMenuOption> options, int startingPage, boolean numberEachEntry) {
        return channel.sendMessageEmbeds(getPaginatedEmbed(channel.getGuild(), options, 25, startingPage, numberEachEntry)).complete();
    }

    private static ReplyCallbackAction menuLogic(SlashCommandInteractionEvent event, List<StringSelectMenuOption> options, int startingPage, boolean numberEachEntry) {
        return event.replyEmbeds(getPaginatedEmbed(event.getGuild(), options, 25, startingPage, numberEachEntry));
    }

    private static List<MenuPage> menuLogic(String msgID, List<StringSelectMenuOption> options) {
        final List<MenuPage> menuPages = new ArrayList<>();

        if (options.size() <= InteractionLimits.SELECTION_MENU) {
            MenuPage menuPage = new MenuPage();
            for (final var option : options)
                menuPage.addOption(option);
            menuPages.add(menuPage);
        } else {

            final int pagesRequired = (int) Math.ceil((double) options.size() / InteractionLimits.SELECTION_MENU);
            final int pageControllers = 1 + (int) Math.ceil(((pagesRequired - 1) * (4)) / 2.0);
            final int actualPagesRequired = (int) Math.ceil((double) (options.size() + pageControllers) / InteractionLimits.SELECTION_MENU);

            int lastIndex = 0;
            for (int i = 0; i < actualPagesRequired; i++) {
                final MenuPage tempPage = new MenuPage();
                for (int j = 0; j < InteractionLimits.SELECTION_MENU; j++) {
                    if (lastIndex == options.size()) break;

                    if (j == 0 && i != 0) {
                        tempPage.addOption(StringSelectMenuOption.of("Previous Page", "menuPage:previousPage:" + msgID));
                        continue;
                    }

                    if (j == InteractionLimits.SELECTION_MENU - 1) {
                        tempPage.addOption(StringSelectMenuOption.of("Next Page", "menuPage:nextPage:" + msgID));
                        continue;
                    }

                    tempPage.addOption(options.get(lastIndex));
                    lastIndex++;
                }
                menuPages.add(tempPage);
            }
        }

        return menuPages;
    }

    public static List<MenuPage> getMenuPages(long msg) {
        return menuMessages.get(msg);
    }

    @SneakyThrows
    public static SelectMenu getSelectionMenu(User user, List<StringSelectMenuOption> options) {
        return StringSelectionMenuBuilder.of(
                "menuPage:" + user.getIdLong(),
                "Select an option",
                Pair.of(1, 1),
                options.subList(0, Math.min(options.size(), InteractionLimits.SELECTION_MENU))
        ).build();
    }

    public static MessageEmbed getPaginatedEmbed(Guild guild, List<?> content, int maxPerPage, int startingPage) {
        return getPaginatedEmbed(guild, content, maxPerPage, startingPage, false);
    }

    public static void setEmbedStyle(Supplier<EmbedBuilder> supplier) {
        embedStyle = supplier;
    }

    public static MessageEmbed getPaginatedEmbed(Guild guild, List<?> content, int maxPerPage, int startingPage, boolean numberEachEntry) {
        EmbedBuilder eb = embedStyle.get().appendDescription("\t");

        int index = (startingPage * (maxPerPage == 0 ? maxPerPage : maxPerPage - 1)) + 1;
        for (int j = 0; j < content.size(); j++) {
            if (j == maxPerPage) break;

            eb.appendDescription((numberEachEntry ? "**" + (index++) + ".** - " : "") + content.get(j) + "\n");
        }

        return eb.build();
    }

}
