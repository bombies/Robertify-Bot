package main.commands.commands.dev;

import main.commands.CommandContext;
import main.commands.IDevCommand;
import main.constants.Toggles;
import main.main.Robertify;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.toggles.TogglesConfig;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AnnouncementCommand extends ListenerAdapter implements IDevCommand {
    private final Logger logger = LoggerFactory.getLogger(AnnouncementCommand.class);

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final var guild = ctx.getGuild();

        if (args.isEmpty()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Provide an announcement!").build()).queue();
        } else {
            String announcement = String.join(" ", args).replaceAll("\\\\n", "\n");

            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "**Are you sure you want to send this announcement?**\n\n" + announcement).build())
                    .setActionRow(
                            Button.of(ButtonStyle.SUCCESS, "devannouncement:yes", "Yes"),
                            Button.of(ButtonStyle.DANGER, "devannouncement:no", "No")
                    )
                    .queue();
        }
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        if (!event.getButton().getId().startsWith("devannouncement:")) return;

        final var g = event.getGuild();

        switch (event.getButton().getId().split(":")[1]) {
            case "yes" -> {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(g, "Now sending announcement to all guilds!").build())
                        .queue(success -> {
                            event.getMessage().editMessageComponents(ActionRow.of(
                                            Button.of(ButtonStyle.SUCCESS, "devannouncement:yes", "Yes").asDisabled(),
                                            Button.of(ButtonStyle.DANGER, "devannouncement:no", "No").asDisabled()
                                    )
                            ).queue(buttonsChanged -> {
                                for (var guild : Robertify.api.getGuilds()) {
                                    if (!new TogglesConfig().getToggle(guild, Toggles.GLOBAL_ANNOUNCEMENTS))
                                        continue;

                                    GuildConfig guildConfig = new GuildConfig();

                                    final long channelID;

                                    if (guild.getCommunityUpdatesChannel() == null) {
                                        if (!guildConfig.announcementChannelIsSet(guild.getIdLong())) {
                                            continue;
                                        } else {
                                            channelID = guildConfig.getAnnouncementChannelID(guild.getIdLong());
                                        }
                                    } else {
                                        channelID = guild.getCommunityUpdatesChannel().getIdLong();
                                    }

                                    try {
                                        guild.getTextChannelById(channelID)
                                                .sendMessageEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild,
                                                                "❗ IMPORTANT: Global Announcement",
                                                                        "Announcement from: " + event.getUser().getAsMention() + "\n\n"
                                                                                + event.getMessage().getEmbeds().get(0).getDescription().replaceFirst("\\*\\*Are you sure you want to send this announcement\\?\\*\\*\n\n", "")
                                                                )
                                                                .setFooter("You can toggle global announcements with \"toggle globalannouncements\"")
                                                                .setTimestamp(Instant.now())
                                                                .build()
                                                ).queueAfter(1, TimeUnit.SECONDS);
                                    } catch (InsufficientPermissionException e) {
                                        logger.error("Was not able to send a changelog in {}", guild.getName());
                                    }
                                }
                            });
                        });
            }
            case "no" -> {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(g, "The announcement will not be sent!").build())
                        .queue(success -> {
                            event.getMessage().editMessageComponents(ActionRow.of(
                                        Button.of(ButtonStyle.SUCCESS, "devannouncement:yes", "Yes").asDisabled(),
                                        Button.of(ButtonStyle.DANGER, "devannouncement:no", "No").asDisabled()
                                    )
                            ).queue();
                        });
            }
        }
    }

    @Override
    public String getName() {
        return "announce";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }
}
