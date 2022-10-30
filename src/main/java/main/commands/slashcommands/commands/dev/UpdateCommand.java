package main.commands.slashcommands.commands.dev;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.IDevCommand;
import main.commands.slashcommands.commands.management.dedicatedchannel.DedicatedChannelCommand;
import main.main.Robertify;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.database.mongodb.AbstractMongoDatabase;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.List;

public class UpdateCommand extends AbstractSlashCommand implements IDevCommand {
    private final Logger logger = LoggerFactory.getLogger(UpdateCommand.class);
    
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final var guild = ctx.getGuild();

        if (args.isEmpty()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide arguments!").build())
                    .queue();
            return;
        }

        switch (args.get(0).toLowerCase()) {
            case "db" -> {
                AbstractMongoDatabase.initAllCaches();
                msg.addReaction(Emoji.fromFormatted("✅")).queue();
            }
            case "dedichannel", "dc" -> handleDedicatedChannelUpdates(msg, args);
            default -> msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Invalid args!").build()).queue();
        }
    }

    public void handleDedicatedChannelUpdates(Message msg, List<String> args) {
        final var guild = msg.getGuild();

        if (args.size() < 2) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide more arguments!").build()).queue();
            return;
        }

        try {
            switch (args.get(1).toLowerCase()) {
                case "all" -> {
                    for (Guild g : Robertify.shardManager.getGuilds()) {
                        final var config = new DedicatedChannelConfig(g);
                        config.updateButtons();
                        config.updateTopic();
                        config.updateMessage();
                    }
                }
                case "topic" -> DedicatedChannelConfig.updateAllTopics();
                case "buttons" -> DedicatedChannelConfig.updateAllButtons();
                case "message" -> {
                    for (Guild g : Robertify.shardManager.getGuilds()) {
                        final var config = new DedicatedChannelConfig(g);
                        config.updateMessage();
                    }
                }
                default -> msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Invalid arg").build()).queue();
            }
            msg.addReaction(Emoji.fromFormatted("✅")).queue();
        } catch (Exception e) {
            logger.error("[FATAL ERROR] An unexpected error occurred!", e);
            msg.addReaction(Emoji.fromFormatted("❌")).queue();
        }
    }

    @Override
    public String getName() {
        return "update";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("update")
                        .setDescription("Update certain things on Robertify!")
                        .addSubCommands(
                                SubCommand.of(
                                        "all",
                                        "Update everything"
                                ),
                                SubCommand.of(
                                        "topic",
                                        "Update the topic of all request channels"
                                ),
                                SubCommand.of(
                                        "buttons",
                                        "Update the buttons in all request channels"
                                ),
                                SubCommand.of(
                                        "message",
                                        "Update the message in all request channels"
                                ),
                                SubCommand.of(
                                        "clean",
                                        "Clean all the request channels"
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


        final Guild guild = event.getGuild();

        switch (event.getSubcommandName()) {
            case "all" -> {
                for (Guild g : Robertify.shardManager.getGuilds()) {
                    final var conf = new DedicatedChannelConfig(g);
                    conf.updateButtons();
                    conf.updateTopic();
                    conf.updateMessage();
                }

                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Successfully updated everything").build())
                        .setEphemeral(true)
                        .queue();
            }
            case "topic" -> {
                DedicatedChannelConfig.updateAllTopics();
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Successfully updated all topics").build())
                        .setEphemeral(true)
                        .queue();
            }
            case "buttons" -> {
                DedicatedChannelConfig.updateAllButtons();
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Successfully updated all buttons").build())
                        .setEphemeral(true)
                        .queue();
            }
            case "message" -> {
                for (Guild g : Robertify.shardManager.getGuilds())
                    new DedicatedChannelConfig(g).updateMessage();
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Successfully updated all messages").build())
                        .setEphemeral(true)
                        .queue();
            }
            case "clean" -> {
                event.deferReply().queue();
                for (Guild g : Robertify.shardManager.getGuilds())
                    new DedicatedChannelConfig(g).cleanChannel();
                event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Successfully cleaned all channels").build())
                        .setEphemeral(true)
                        .queue();
            }
        }
    }
}
