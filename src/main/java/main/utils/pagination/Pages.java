package main.utils.pagination;

import lombok.SneakyThrows;
import main.constants.InteractionLimits;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.selectionmenu.SelectMenuOption;
import main.utils.component.interactions.selectionmenu.SelectionMenuBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public abstract class Pages {
    private static final HashMap<Long, List<MessagePage>> messages = new HashMap<>();
    private static final HashMap<Long, List<MenuPage>> menuMessages = new HashMap<>();
    private static Supplier<EmbedBuilder> embedStyle = EmbedBuilder::new;

    private static final Paginator paginator = Paginator.getDefaultPaginator();

    public static Message paginateMessage(TextChannel channel, User user, List<MessagePage> messagePages) {
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

        WebhookMessageCreateAction<Message> messageAction = event.getHook().sendMessageEmbeds(messagePages.get(0).getEmbed()).setEphemeral(false);

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

    public static Message paginateMessage(TextChannel channel, User user, List<String> content, int maxPerPage) {
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

    private static void messageLogic(Guild guild, List<MessagePage> messagePages, List<String> content, int maxPerPage) {
        if (content.size() <= maxPerPage) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "\t");
            for (String str : content)
                eb.appendDescription(str + "\n");
            messagePages.add(new MessagePage(eb.build()));
        } else {
            int pagesRequired = (int)Math.ceil((double)content.size() / maxPerPage);

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

    public static List<MessagePage> getMessagePages(long msg) {
        return messages.get(msg);
    }

    @SneakyThrows
    public static void paginateMenu(User user, Message msg, List<SelectMenuOption> options) {
        List<MenuPage> menuPages = menuLogic(msg.getId(), options);

        final var firstPage = menuPages.get(0);

        SelectMenu menu = SelectionMenuBuilder.of(
                "menupage:" + user.getId(),
                "Select an option",
                Pair.of(1, 1),
                firstPage.getOptions().subList(0, Math.min(options.size(), InteractionLimits.SELECTION_MENU))
        ).build();

        msg.editMessageComponents(ActionRow.of(menu))
                .queue(success -> menuMessages.put(msg.getIdLong(), menuPages));
    }

    @SneakyThrows
    public static void paginateMenu(User user, ReplyCallbackAction msg, List<SelectMenuOption> options) {
        List<MenuPage> menuPages = menuLogic("null", options);

        final var firstPage = menuPages.get(0);

        SelectMenu menu = SelectionMenuBuilder.of(
                "menupage:" + user.getId(),
                "Select an option",
                Pair.of(1, 1),
                firstPage.getOptions().subList(0, Math.min(options.size(), InteractionLimits.SELECTION_MENU))
        ).build();

        msg.addActionRow(menu)
                .setEphemeral(false)
                .queue(success -> success.retrieveOriginal().queue(og -> menuMessages.put(og.getIdLong(), menuPages)));
    }

    public static void paginateMenu(TextChannel channel, User user, List<SelectMenuOption> options, int startingPage) {
        paginateMenu(channel, user, options, startingPage, false);
    }

    public static void paginateMenu(TextChannel channel, User user, List<SelectMenuOption> options, int startingPage, boolean numberEachEntry) {
        Message msg = menuLogic(channel, options, startingPage, numberEachEntry);
        paginateMenu(user, msg, options);
    }

    public static void paginateMenu(SlashCommandInteractionEvent event, List<SelectMenuOption> options, int startingPage, boolean numberEachEntry) {
        final var msg = menuLogic(event, options, startingPage, numberEachEntry);
        paginateMenu(event.getUser(), msg, options);
    }

    private static Message menuLogic(TextChannel channel, int startingPage, List<SelectMenuOption> options) {
        return menuLogic(channel, options, startingPage, false);
    }

    private static ReplyCallbackAction menuLogic(SlashCommandInteractionEvent event, int startingPage, List<SelectMenuOption> options) {
        return menuLogic(event, options, startingPage, false);
    }

    private static Message menuLogic(TextChannel channel, List<SelectMenuOption> options, int startingPage, boolean numberEachEntry) {
        return channel.sendMessageEmbeds(getPaginatedEmbed(channel.getGuild(), options, 25, startingPage, numberEachEntry)).complete();
    }

    private static ReplyCallbackAction menuLogic(SlashCommandInteractionEvent event, List<SelectMenuOption> options, int startingPage, boolean numberEachEntry) {
        return event.replyEmbeds(getPaginatedEmbed(event.getGuild(), options, 25, startingPage, numberEachEntry));
    }

    private static List<MenuPage> menuLogic(String msgID, List<SelectMenuOption> options) {
        final List<MenuPage> menuPages = new ArrayList<>();

        if (options.size() <= InteractionLimits.SELECTION_MENU) {
            MenuPage menuPage = new MenuPage();
            for (final var option : options)
                menuPage.addOption(option);
            menuPages.add(menuPage);
        } else {

            final int pagesRequired = (int)Math.ceil((double)options.size() / InteractionLimits.SELECTION_MENU);
            final int pageControllers = 1 + (int)Math.ceil(((pagesRequired-1) * (4))/2.0);
            final int actualPagesRequired = (int)Math.ceil((double)(options.size() + pageControllers) / InteractionLimits.SELECTION_MENU);

            int lastIndex = 0;
            for (int i = 0; i < actualPagesRequired; i++) {
                final MenuPage tempPage = new MenuPage();
                for (int j = 0; j < InteractionLimits.SELECTION_MENU; j++) {
                    if (lastIndex == options.size()) break;

                    if (j == 0 && i != 0) {
                        tempPage.addOption(SelectMenuOption.of("Previous Page", "menuPage:previousPage:" + msgID));
                        continue;
                    }

                    if (j == InteractionLimits.SELECTION_MENU - 1) {
                        tempPage.addOption(SelectMenuOption.of("Next Page", "menuPage:nextPage:" + msgID));
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
    public static SelectMenu getSelectionMenu(User user, List<SelectMenuOption> options) {
        return SelectionMenuBuilder.of(
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

        int index = (startingPage * (maxPerPage == 0 ? maxPerPage : maxPerPage-1)) + 1;
        for (int j = 0; j < content.size(); j++) {
            if (j == maxPerPage) break;

            eb.appendDescription((numberEachEntry ? "**" + (index++) + ".** - ": "") + content.get(j) + "\n");
        }

        return eb.build();
    }

}
