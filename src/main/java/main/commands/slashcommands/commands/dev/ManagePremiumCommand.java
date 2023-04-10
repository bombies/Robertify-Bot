package main.commands.slashcommands.commands.dev;

import main.main.Robertify;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ManagePremiumCommand extends AbstractSlashCommand {
    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("managepremium")
                        .setDescription("Manage premium for users and servers!")
                        .addSubCommands(
                                SubCommand.of(
                                        "add",
                                        "Add a new premium user",
                                        List.of(
                                                CommandOption.of(
                                                        OptionType.STRING,
                                                        "user",
                                                        "The user to add",
                                                        true
                                                ),
                                                CommandOption.of(
                                                        OptionType.STRING,
                                                        "tier",
                                                        "The tier to set for the user",
                                                        true,
                                                        List.of("BRONZE", "SILVER", "GOLD", "DIAMOND", "EMERALD")
                                                ),
                                                CommandOption.of(
                                                        OptionType.STRING,
                                                        "expiration",
                                                        "The date in milliseconds for this subscription to expire",
                                                        false
                                                )
                                        )
                                ),
                                SubCommand.of(
                                        "remove",
                                        "Remove a user from premium",
                                        List.of(
                                                CommandOption.of(
                                                        OptionType.STRING,
                                                        "user",
                                                        "The user to remove premium from",
                                                        true
                                                )
                                        )
                                ),
                                SubCommand.of(
                                        "update",
                                        "Update a user's premium tier",
                                        List.of(
                                                CommandOption.of(
                                                        OptionType.STRING,
                                                        "user",
                                                        "The user to add",
                                                        true
                                                ),
                                                CommandOption.of(
                                                        OptionType.STRING,
                                                        "tier",
                                                        "The tier to set for the user",
                                                        true,
                                                        List.of("BRONZE", "SILVER", "GOLD", "DIAMOND", "EMERALD")
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
        if (!checks(event)) return;
        final var userID = Long.parseLong(event.getOption("user").getAsString());
        final var robertifyAPI = Robertify.getRobertifyAPI();
        final var guild = event.getGuild();

        if (robertifyAPI == null) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "The API hasn't been initialized.").build())
                    .queue();
            return;
        }

        event.replyEmbeds(
                RobertifyEmbedUtils.embedMessage(RobertifyLocaleMessage.GeneralMessages.DISABLED_COMMAND)
                        .build()
        ).queue();
//        switch (event.getSubcommandName()) {
//            case "add" -> {
//                final var tier = getTierCode(event.getOption("tier").getAsString());
//                final var expiration = event.getOption("expiration") != null ? Long.parseLong(event.getOption("expiration").getAsString()) : -1;
//
//                try {
//                    if (expiration != -1) {
//                        robertifyAPI.addPremiumUser(userID, 0, tier, expiration);
//
//                        Robertify.getShardManager().retrieveUserById(userID).queue(user -> event.getHook().sendMessageEmbeds(
//                                        RobertifyEmbedUtils.embedMessage(
//                                                guild,
//                                                "You have successfully added " + user.getName() + "#" + user.getDiscriminator() + " as a premium user until `"+ GeneralUtils.formatDate(expiration, TimeFormat.DD_M_YYYY_HH_MM_SS) +"`."
//                                        ).build())
//                                .queue()
//                        );
//                    } else {
//                        robertifyAPI.addPremiumUser(userID, 0, tier, 32503680000000L);
//                        Robertify.getShardManager().retrieveUserById(userID).queue(user -> event.getHook().sendMessageEmbeds(
//                                        RobertifyEmbedUtils.embedMessage(
//                                                guild,
//                                                "You have successfully added " + user.getName() + "#" + user.getDiscriminator() + " as a premium user permanently."
//                                        ).build())
//                                .queue()
//                        );
//
//                    }
//                } catch (IllegalArgumentException e) {
//                    event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, e.getMessage()).build())
//                            .queue();
//                }
//            }
//            case "remove" -> {
//                try {
//                    robertifyAPI.deletePremiumUser(userID);
//
//                    Robertify.getShardManager().retrieveUserById(userID).queue(user -> event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(
//                                    guild,
//                                    "You have successfully removed " + user.getName() + "#" + user.getDiscriminator() + " as a premium user."
//                            ).build())
//                            .queue()
//                    );
//                } catch (IllegalArgumentException e) {
//                    event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, e.getMessage()).build())
//                            .queue();
//                }
//            }
//            case "update" -> {
//                final var tier = getTierCode(event.getOption("tier").getAsString());
//
//                try {
//                    robertifyAPI.updateUserTier(userID, tier);
//                    Robertify.getShardManager().retrieveUserById(userID).queue(user -> event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(
//                            guild,
//                            "You have successfully updated " + user.getName() + "#" + user.getDiscriminator() + "'s premium tier to **" + event.getOption("tier").getAsString() + "**!"
//                        ).build()).queue()
//                    );
//
//                } catch (IllegalArgumentException e) {
//                    event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, e.getMessage()).build())
//                            .queue();
//                }
//            }
//        }
    }

    private int getTierCode(String tier) {
        switch (tier.toLowerCase()) {
            case "bronze" -> {
                return 0;
            }
            case "silver" -> {
                return 1;
            }
            case "gold" -> {
                return 2;
            }
            case "diamond" -> {
                return 3;
            }
            case "emerald" -> {
                return 4;
            }
            default -> throw new IllegalArgumentException("That tier is invalid!");
        }
    }
}
