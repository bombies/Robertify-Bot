package main.commands.slashcommands.commands.util;

import main.constants.TimeFormat;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.database.mongodb.cache.BotBDCache;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
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

        if (!latestAlert.getLeft().isEmpty() && !latestAlert.getLeft().isBlank())
            botDB.addAlertViewer(user.getIdLong());

        event.replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(event.getGuild(),
                "Alert From The Developer",
                (latestAlert.getLeft().isEmpty() || latestAlert.getLeft().isBlank()) ?
                        "There is no alert..." : latestAlert.getLeft()
        )
                        .setFooter("You are #" + botDB.getPosOfAlertViewer(user.getIdLong()) + " to view this alert! • " + GeneralUtils.formatDate(latestAlert.getRight(), TimeFormat.DD_MMMM_YYYY))
                .build()).queue();

    }
}
