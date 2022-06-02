package main.commands.slashcommands.commands.util;

import main.constants.TimeFormat;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.database.mongodb.cache.BotBDCache;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public class AlertCommand extends AbstractSlashCommand {
    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("alert")
                        .setDescription("View the latest alert from the developer!")
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

        final var botDB = BotBDCache.getInstance();
        final var user = event.getUser();
        final var latestAlert = botDB.getLatestAlert();
        final var localeManager = LocaleManager.getLocaleManager(event.getGuild());

        if (!latestAlert.getLeft().isEmpty() && !latestAlert.getLeft().isBlank())
            botDB.addAlertViewer(user.getIdLong());

        event.replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(event.getGuild(),
                        RobertifyLocaleMessage.AlertMessages.ALERT_EMBED_TITLE,
                (latestAlert.getLeft().isEmpty() || latestAlert.getLeft().isBlank()) ?
                        localeManager.getMessage(RobertifyLocaleMessage.AlertMessages.NO_ALERT) : latestAlert.getLeft()
        )
                        .setFooter(localeManager.getMessage(RobertifyLocaleMessage.AlertMessages.ALERT_EMBED_FOOTER,
                                Pair.of("{number}", String.valueOf(botDB.getPosOfAlertViewer(user.getIdLong()))),
                                Pair.of("{alertDate}", GeneralUtils.formatDate(latestAlert.getRight(), TimeFormat.DD_MMMM_YYYY))
                        ))
                .build()).queue();

    }
}
