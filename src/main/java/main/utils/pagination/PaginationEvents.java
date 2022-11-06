package main.utils.pagination;

import main.commands.slashcommands.commands.audio.FavouriteTracksCommand;
import main.constants.MessageButton;
import main.utils.RobertifyEmbedUtils;
import main.utils.database.mongodb.cache.FavouriteTracksCache;
import main.utils.json.themes.ThemesConfig;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.AttachedFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;

public class PaginationEvents extends ListenerAdapter {
    private static final HashMap<Long, Integer> currentPage = new HashMap<>();

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getButton().getId().startsWith(MessageButton.PAGE_ID.toString())) {
            event.getHook().setEphemeral(true);

            if (!currentPage.containsKey(event.getMessage().getIdLong()))
                currentPage.put(event.getMessage().getIdLong(), 0);

            long msg = event.getMessage().getIdLong();
            List<MessagePage> messagePages = Pages.getMessagePages(msg);

            if (messagePages == null) {
                event.deferEdit().queue();
                return;
            }

            if (event.getButton().getId().equals(MessageButton.FRONT + event.getUser().getId())) {
                currentPage.put(msg, 0);
                event.editMessageEmbeds(messagePages.get(0).getEmbed())
                        .setComponents(((currentPage.get(msg) == 0) ?
                                Paginator.getButtons(event.getUser(), false, false, true, true) :
                                Paginator.getButtons(event.getUser())))
                        .queue();
            } else if (event.getButton().getId().equals(MessageButton.PREVIOUS + event.getUser().getId())) {
                currentPage.put(msg, currentPage.get(msg) - 1);
                event.editMessageEmbeds(messagePages.get(currentPage.get(msg)).getEmbed())
                        .setComponents(((currentPage.get(msg) == 0) ?
                                Paginator.getButtons(event.getUser(), false, false, true, true) :
                                Paginator.getButtons(event.getUser())))
                        .queue();
            } else if (event.getButton().getId().equals(MessageButton.NEXT + event.getUser().getId())) {
                currentPage.put(msg, currentPage.get(msg) + 1);
                event.editMessageEmbeds(messagePages.get(currentPage.get(msg)).getEmbed())
                        .setComponents(((currentPage.get(msg) == messagePages.size()-1) ?
                                Paginator.getButtons(event.getUser(), true, true, false, false) :
                                Paginator.getButtons(event.getUser())))
                        .queue();
            } else if (event.getButton().getId().equals(MessageButton.END + event.getUser().getId())) {
                currentPage.put(msg, messagePages.size()-1);
                event.editMessageEmbeds(messagePages.get(currentPage.get(msg)).getEmbed())
                        .setComponents(((currentPage.get(msg) == messagePages.size()-1) ?
                                Paginator.getButtons(event.getUser(), true, true, false, false) :
                                Paginator.getButtons(event.getUser()))).queue();
            } else {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(event.getGuild(), RobertifyLocaleMessage.GeneralMessages.NO_PERMS_BUTTON);
                event.replyEmbeds(eb.build()).setEphemeral(true).queue();
            }

        } else if (event.getButton().getId().startsWith( "queue:" + MessageButton.PAGE_ID)) {
            event.getHook().setEphemeral(true);

            if (!currentPage.containsKey(event.getMessage().getIdLong()))
                currentPage.put(event.getMessage().getIdLong(), 0);

            long msg = event.getMessage().getIdLong();
            final var queuePages = Pages.getQueuePages(msg);

            if (queuePages == null) {
                event.deferEdit().queue();
                return;
            }

            if (event.getButton().getId().equals("queue:" + MessageButton.FRONT + event.getUser().getId())) {
                currentPage.put(msg, 0);
                event.editMessageAttachments(AttachedFile.fromData(queuePages.get(0).getImage()))
                        .setComponents(((currentPage.get(msg) == 0) ?
                                Paginator.getQueueButtons(event.getUser(), false, false, true, true) :
                                Paginator.getQueueButtons(event.getUser())))
                        .queue();
            } else if (event.getButton().getId().equals("queue:" + MessageButton.PREVIOUS + event.getUser().getId())) {
                currentPage.put(msg, currentPage.get(msg) - 1);
                event.editMessageAttachments(AttachedFile.fromData(queuePages.get(currentPage.get(msg)).getImage()))
                        .setComponents(((currentPage.get(msg) == 0) ?
                                Paginator.getQueueButtons(event.getUser(), false, false, true, true) :
                                Paginator.getQueueButtons(event.getUser())))
                        .queue();
            } else if (event.getButton().getId().equals("queue:" + MessageButton.NEXT + event.getUser().getId())) {
                currentPage.put(msg, currentPage.get(msg) + 1);
                event.editMessageAttachments(AttachedFile.fromData(queuePages.get(currentPage.get(msg)).getImage()))
                        .setComponents(((currentPage.get(msg) == queuePages.size()-1) ?
                                Paginator.getQueueButtons(event.getUser(), true, true, false, false) :
                                Paginator.getQueueButtons(event.getUser())))
                        .queue();
            } else if (event.getButton().getId().equals("queue:" + MessageButton.END + event.getUser().getId())) {
                currentPage.put(msg, queuePages.size()-1);
                event.editMessageAttachments(AttachedFile.fromData(queuePages.get(currentPage.get(msg)).getImage()))
                        .setComponents(((currentPage.get(msg) == queuePages.size()-1) ?
                                Paginator.getQueueButtons(event.getUser(), true, true, false, false) :
                                Paginator.getQueueButtons(event.getUser()))).queue();
            } else {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(event.getGuild(), RobertifyLocaleMessage.GeneralMessages.NO_PERMS_BUTTON);
                event.replyEmbeds(eb.build()).setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onSelectMenuInteraction(@NotNull SelectMenuInteractionEvent event) {
        if (!event.getComponentId().startsWith("menupage")) return;

        final var guild = event.getGuild();
        if (!event.getUser().getId().equals(event.getComponentId().split(":")[1])) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You do not have permission to interact with this selection menu").build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!currentPage.containsKey(event.getMessage().getIdLong()))
            currentPage.put(event.getMessage().getIdLong(), 0);

        final var msg = event.getMessage();

        int currentPage = PaginationEvents.currentPage.get(msg.getIdLong());
        switch (event.getSelectedOptions().get(0).getValue().split(":")[1]) {
            case "nextPage" -> PaginationEvents.currentPage.put(msg.getIdLong(), currentPage + 1);
            case "previousPage" -> PaginationEvents.currentPage.put(msg.getIdLong(), currentPage - 1);
            default -> {
                return;
            }
        }

        currentPage = PaginationEvents.currentPage.get(msg.getIdLong());

        MenuPage menuPage = Pages.getMenuPages(msg.getIdLong())
                .get(currentPage);

        int finalCurrentPage = currentPage;
        event.editSelectMenu(
                Pages.getSelectionMenu(
                        event.getUser(),
                        menuPage.getOptions()
                )
        ).queue(s -> {
            final var tracks = FavouriteTracksCache.getInstance().getTracks(event.getMember().getIdLong());
            final var theme = new ThemesConfig(guild).getTheme();
            FavouriteTracksCommand.setDefaultEmbed(event.getMember(), tracks, theme);
            msg.editMessageEmbeds(Pages.getPaginatedEmbed(msg.getGuild(), menuPage.toStringList(), 25, finalCurrentPage, true))
                    .queue();
        });
    }
}
