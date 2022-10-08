package main.commands.slashcommands.commands.dev;

import main.main.Robertify;
import main.utils.RobertifyEmbedUtils;
import main.utils.apis.robertify.models.RobertifyPremium;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ResetPremiumFeaturesCommand extends AbstractSlashCommand {
    private final Logger logger = LoggerFactory.getLogger(ResetPremiumFeaturesCommand.class);

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("resetpremiumfeatures")
                        .setDescription("Reset premium features for a specific guild or all guilds")
                        .addSubCommands(
                                SubCommand.of(
                                        "all",
                                        "Reset premium features for all guilds"
                                ),
                                SubCommand.of(
                                        "guild",
                                        "Reset premium features for a specific guild",
                                        List.of(
                                                CommandOption.of(
                                                        OptionType.NUMBER,
                                                        "guildid",
                                                        "The ID of the guild",
                                                        true
                                                )
                                        )
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
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!devCheck(event)) return;

        final var cmdGuild = event.getGuild();
        event.deferReply().queue();

        try {
            switch (event.getSubcommandName()) {
                case "all" -> {
                    Robertify.getShardManager()
                            .getGuildCache()
                            .forEach(g -> {
                                RobertifyPremium.resetPremiumFeatures(g);
                                logger.info("Reset all premium features for {}", g.getName());
                            });
                }
                case "guild" -> {
                    final var guildID = event.getOption("guildid").getAsLong();
                    final var guild = Robertify.getShardManager().getGuildById(guildID);
                    if (guild == null) {
                        event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(cmdGuild, "There was no such guild with the ID: " + guildID).build())
                                .setEphemeral(true)
                                .queue();
                        return;
                    }

                    RobertifyPremium.resetPremiumFeatures(guild);
                    logger.info("Reset all premium features for {}", guild.getName());
                    event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(cmdGuild, "Successfully reset all premium features for " + guild.getName()).build())
                            .setEphemeral(true)
                            .queue();
                }
            }
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(
                    cmdGuild, RobertifyLocaleMessage.GeneralMessages.UNEXPECTED_ERROR
            ).build())
                    .setEphemeral(true)
                    .queue();
        }
    }
}
