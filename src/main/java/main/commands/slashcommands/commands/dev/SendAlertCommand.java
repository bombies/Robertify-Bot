package main.commands.slashcommands.commands.dev;

import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.database.mongodb.cache.BotBDCache;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

public class SendAlertCommand extends AbstractSlashCommand {
    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("sendalert")
                        .setDescription("Send an alert to all users")
                        .addOptions(
                                CommandOption.of(
                                        OptionType.STRING,
                                        "alert",
                                        "The alert to be sent",
                                        true
                                )
                        )
                        .setDevCommand()
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

        final var alert = event.getOption("alert").getAsString();
        BotBDCache.getInstance().setLatestAlert(alert);
        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "You have sent out a new alert!").build())
                .setEphemeral(true)
                .queue();
    }
}
