package main.commands.slashcommands.commands.dev;

import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.database.mongodb.cache.BotBDCache;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
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
        if (!devCheck(event)) return;

        final var alert = event.getOption("alert").getAsString();

        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "Are you sure you want to send out this alert?\n\n" + alert.replaceAll("\\n", "\n")).build())
                        .addActionRow(
                                Button.of(ButtonStyle.SUCCESS, "sendalert:yes", "Yes"),
                                Button.of(ButtonStyle.DANGER, "sendalert:no", "No")
                        )
                        .setEphemeral(false)
                        .queue();
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        if (!event.getButton().getId().startsWith("sendalert:"))
            return;

        final Guild guild = event.getGuild();
        if (!BotBDCache.getInstance().isDeveloper(event.getMember().getIdLong())) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NO_PERMS_BUTTON).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String[] split = event.getButton().getId().split(":");
        final var disabledButtons = event.getMessage().getButtons()
                .stream()
                .map(Button::asDisabled)
                .toList();

        switch (split[1].toLowerCase()) {
            case "yes" -> {
                final var alert = event.getMessage().getEmbeds().get(0).getDescription().replaceAll("Are you sure you want to send out this alert\\?\n\n", "");
                BotBDCache.getInstance().setLatestAlert(alert);
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have sent out a new alert!").build())
                        .setEphemeral(true)
                        .queue();
            }
            case "no" -> event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Okay! I will not send out that alert.").build())
                    .setEphemeral(true)
                    .queue();
        }
        event.getMessage().editMessageComponents(ActionRow.of(disabledButtons)).queue();
    }
}
