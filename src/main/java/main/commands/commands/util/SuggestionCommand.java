package main.commands.commands.util;

import lombok.SneakyThrows;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.InteractiveCommand;
import main.utils.database.mongodb.cache.BotInfoCache;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SuggestionCommand extends InteractiveCommand implements ICommand {
    private final static Logger logger = LoggerFactory.getLogger(SuggestionCommand.class);

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();

        if (args.isEmpty()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(msg.getGuild(), "You must provide a suggestion!").build())
                    .queue();
        } else {
            switch (args.get(0).toLowerCase()) {
                case "setup" -> setup(msg);
                case "accept" -> accept(msg, args);
                case "deny", "reject" -> deny(msg, args);
                case "ban" -> ban(msg, args);
                case "unban" -> unban(msg, args);
                default -> sendSuggestion(msg, args);
            }
        }
    }

    private void setup(Message msg) {
        if (!isDeveloper(msg.getAuthor())) return;

        final var config = BotInfoCache.getInstance();

        if (config.isSuggestionsSetup()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(msg.getGuild(), "The suggestions channels have already been setup!").build())
                    .queue();
            return;
        }

        final Guild guild = msg.getGuild();

        guild.createCategory("Suggestions").queue(category -> {

           category.upsertPermissionOverride(guild.getPublicRole())
                   .setDeny(Permission.VIEW_CHANNEL)
                   .queueAfter(1, TimeUnit.SECONDS);

           guild.createTextChannel("pending-suggestions", category)
                   .queueAfter(2, TimeUnit.SECONDS, pendingChannel -> {
                       guild.createTextChannel("accepted-suggestions", category)
                               .queueAfter(1, TimeUnit.SECONDS, acceptedChannel -> {
                                  guild.createTextChannel("denied-suggestions", category)
                                          .queueAfter(1, TimeUnit.SECONDS, deniedChannel -> {
                                              config.initSuggestionChannels(category.getIdLong(), pendingChannel.getIdLong(), acceptedChannel.getIdLong(), deniedChannel.getIdLong());
                                                msg.addReaction("✅").queue();
                                          });
                               });
                   });
        });

    }

    private void sendSuggestion(Message msg, List<String> args) {
        msg.replyEmbeds(handleSuggestion(msg.getGuild(), msg.getAuthor(), String.join(" ", args)))
                .queue(sentMessage -> {
                    msg.delete().queueAfter(10, TimeUnit.SECONDS);
                    sentMessage.delete().queueAfter(11, TimeUnit.SECONDS);
                });
    }

    private void accept(Message msg, List<String> args) {
        if (!isDeveloper(msg.getAuthor())) return;

        final var guild = msg.getGuild();

        if (args.size() < 2) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide the ID of a suggestion to accept").build())
                    .queue();
            return;
        }

        String id = args.get(1);

        if (!GeneralUtils.stringIsID(id)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Invalid ID!").build())
                    .queue();
            return;
        }

        final var config = BotInfoCache.getInstance();

        TextChannel pendingChannel = Robertify.api.getTextChannelById(config.getSuggestionsPendingChannelID());

        if (pendingChannel == null) {
            logger.warn("The pending suggestions channel isn't setup!");
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "This feature isn't available right now!").build()).queue();
            return;
        }

        pendingChannel.retrieveMessageById(id).queue(suggestion -> {
            TextChannel acceptedChannel = Robertify.api.getTextChannelById(config.getSuggestionsAcceptedChannelID());

            if (acceptedChannel == null) {
                logger.warn("The pending suggestions channel isn't setup!");
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "This feature isn't available right now!").build()).queue();
                return;
            }
            List<MessageEmbed.Field> embedField = suggestion.getEmbeds().get(0).getFields();
            final String suggester = embedField.get(0).getValue();
            final String pendingSuggestion = embedField.get(1).getValue();

            acceptedChannel.sendMessageEmbeds(getEmbed(
                    new Color(77, 255, 69),
                    suggester,
                    pendingSuggestion,
                    suggestion.getEmbeds().get(0).getThumbnail().getUrl()
            )).queue();

            suggestion.delete().queue();

            GeneralUtils.retrieveUser(GeneralUtils.getDigitsOnly(suggester))
                    .openPrivateChannel().queue(channel -> {
                        final EmbedBuilder acceptedEmbed = new EmbedBuilder();
                        acceptedEmbed.setColor(new Color(77, 255, 69));
                        acceptedEmbed.setTitle("Suggestions");
                        acceptedEmbed.setDescription("**Your suggestion has been accepted!**" +
                                "\nYou will see it appear in the next changelog");
                        acceptedEmbed.addField("Suggestion", pendingSuggestion, false);

                        channel.sendMessageEmbeds(acceptedEmbed.build())
                                .queue(null, new ErrorHandler()
                                        .handle(ErrorResponse.CANNOT_SEND_TO_USER, ignored -> {}));
                    });

            msg.addReaction("✅").queue();
        }, new ErrorHandler()
                .handle(ErrorResponse.UNKNOWN_MESSAGE, e -> {
                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "That ID doesn't belong to any pending suggestion!").build())
                            .queue();
                }));
    }

    private void deny(Message msg, List<String> args) {
        if (!isDeveloper(msg.getAuthor())) return;

        final var guild = msg.getGuild();

        if (args.size() < 2) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide the ID of a suggestion to reject").build())
                    .queue();
            return;
        }

        final String id = args.get(1);
        final String reason;

        if (args.size() >= 3) reason = String.join(" ", args.subList(2, args.size()));
        else reason = null;

        if (!GeneralUtils.stringIsID(id)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Invalid ID!").build())
                    .queue();
            return;
        }

        final var config = BotInfoCache.getInstance();

        TextChannel pendingChannel = Robertify.api.getTextChannelById(config.getSuggestionsPendingChannelID());

        if (pendingChannel == null) {
            logger.warn("The pending suggestions channel isn't setup!");
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "This feature isn't available right now!").build()).queue();
            return;
        }

        pendingChannel.retrieveMessageById(id).queue(suggestion -> {
            TextChannel deniedChannel = Robertify.api.getTextChannelById(config.getSuggestionsDeniedChannelID());

            if (deniedChannel == null) {
                logger.warn("The pending suggestions channel isn't setup!");
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "This feature isn't available right now!").build()).queue();
                return;
            }
            List<MessageEmbed.Field> embedField = suggestion.getEmbeds().get(0).getFields();
            final String suggester = embedField.get(0).getValue();
            final String pendingSuggestion = embedField.get(1).getValue();

            deniedChannel.sendMessageEmbeds(getEmbed(
                    new Color(187, 0, 0),
                    suggester,
                    pendingSuggestion,
                    suggestion.getEmbeds().get(0).getThumbnail().getUrl()
            )).queue();

            suggestion.delete().queue();

            GeneralUtils.retrieveUser(GeneralUtils.getDigitsOnly(suggester))
                    .openPrivateChannel().queue(channel -> {
                        final EmbedBuilder deniedEmbed = new EmbedBuilder();
                        deniedEmbed.setTitle("Suggestions");
                        deniedEmbed.setColor(new Color(187, 0, 0));
                        deniedEmbed.setDescription("**Your suggestion has been denied**");
                        deniedEmbed.addField("Suggestion", pendingSuggestion, false);

                        if (reason != null)
                            deniedEmbed.addField("Reason", reason, false);

                        channel.sendMessageEmbeds(deniedEmbed.build())
                                .queue(null, new ErrorHandler()
                                        .handle(ErrorResponse.CANNOT_SEND_TO_USER, ignored -> {}));
                    });

            msg.addReaction("✅").queue();
        }, new ErrorHandler()
                .handle(ErrorResponse.UNKNOWN_MESSAGE, e -> {
                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "That ID doesn't belong to any pending suggestion!").build())
                            .queue();
                }));
    }

    private MessageEmbed getEmbed(Color color, String suggester, String suggestion, String thumbnail) {
        final EmbedBuilder eb = new EmbedBuilder();

        eb.setColor(color);
        eb.setTitle("Suggestion");
        eb.addField("Suggester",  suggester , false);
        eb.addField("Suggestion", suggestion, false);

        if (thumbnail != null)
            eb.setThumbnail(thumbnail);

        eb.setTimestamp(Instant.now());

        return eb.build();
    }

    private MessageEmbed handleSuggestion(Guild guild, User suggester, String suggestion) {
        if (suggestion.chars().count() > 1024)
            return RobertifyEmbedUtils.embedMessage(guild, "Your suggestion must be no more than 1024 characters!").build();

        final var config = BotInfoCache.getInstance();
        final TextChannel pendingChannel = Robertify.api.getTextChannelById(config.getSuggestionsPendingChannelID());

        if (pendingChannel == null) {
            logger.warn("The suggestion channels aren't setup!");
            return RobertifyEmbedUtils.embedMessage(guild, "This feature isn't available right now!").build();
        }

        if (config.userIsSuggestionBanned(suggester.getIdLong()))
            return RobertifyEmbedUtils.embedMessage(guild, "You have been banned from sending suggestions!").build();

        final EmbedBuilder eb = new EmbedBuilder();

        eb.setColor(new Color(255, 196, 0));
        eb.setTitle("Suggestion from " + suggester.getName());
        eb.addField("Suggester", suggester.getAsMention(), false);
        eb.addField("Suggestion", suggestion, false);
        eb.setThumbnail(suggester.getEffectiveAvatarUrl());
        eb.setTimestamp(Instant.now());

        pendingChannel.sendMessageEmbeds(eb.build()).queue();
        return RobertifyEmbedUtils.embedMessage(guild, "You have successfully sent your suggestion!\n\n" +
                "*Be sure to be on the lookout for a response in your DMs! Ensure your direct messages are enabled" +
                " else you will not receive any updates on your suggestion.*").build();
    }

    private void ban(Message msg, List<String> args) {
        if (!isDeveloper(msg.getAuthor())) return;

        final var guild = msg.getGuild();

        if (args.size() < 2) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide the ID of a user to ban").build())
                    .queue();
            return;
        }

        final String id = args.get(1);

        if (!GeneralUtils.stringIsID(id)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Invalid ID!").build())
                    .queue();
            return;
        }

        final var user = GeneralUtils.retrieveUser(GeneralUtils.getDigitsOnly(id));

        if (user == null) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Invalid user!").build())
                    .queue();
            return;
        }

        try {
            BotInfoCache.getInstance().banSuggestionsUser(user.getIdLong());
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have banned "
                    + user.getAsMention() + "from suggestions").build())
                    .queue();
        } catch (IllegalStateException e) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, e.getMessage()).build()).queue();
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            msg.addReaction("❌").queue();
        }
    }

    private void unban(Message msg, List<String> args) {
        if (!isDeveloper(msg.getAuthor())) return;

        final var guild = msg.getGuild();

        if (args.size() < 2) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide the ID of a user to ban").build())
                    .queue();
            return;
        }

        final String id = args.get(1);

        if (!GeneralUtils.stringIsID(id)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Invalid ID!").build())
                    .queue();
            return;
        }

        final var user = GeneralUtils.retrieveUser(GeneralUtils.getDigitsOnly(id));

        if (user == null) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Invalid user!").build())
                    .queue();
            return;
        }

        try {
            BotInfoCache.getInstance().unbanSuggestionUser(user.getIdLong());
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have banned "
                            + user.getAsMention() + "from suggestions").build())
                    .queue();
        } catch (IllegalStateException e) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, e.getMessage()).build()).queue();
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            msg.addReaction("❌").queue();
        }
    }

    @SneakyThrows
    private boolean isDeveloper(User user) {
        return BotInfoCache.getInstance().isDeveloper(user.getIdLong());
    }

    @Override
    public String getName() {
        return "suggest";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`\n" +
                "Want to see something added to Robertify? Suggest it using this command!\n\n" +
                "**Usage**: `"+ prefix +"suggest <suggestion>`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("sg", "sug");
    }

    @Override
    public void initCommand() {

    }

    @Override
    public void initCommand(Guild g) {
        setInteractionCommand(getCommand());
        upsertCommand(g);
    }

    public InteractionCommand getCommand() {
        return InteractionCommand.create()
                .setCommand(Command.of(
                        getName(),
                        "Suggest a feature you'd like to see in the bot!",
                        List.of(CommandOption.of(
                                OptionType.STRING,
                                "suggestion",
                                "The suggestion to send to us",
                                true
                        ))
                )).build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(getName())) return;

        event.replyEmbeds(handleSuggestion(event.getGuild(), event.getUser(), event.getOption("suggestion").getAsString()))
                .setEphemeral(true)
                .queue();
    }
}
