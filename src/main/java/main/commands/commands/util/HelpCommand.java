package main.commands.commands.util;

import lombok.SneakyThrows;
import main.commands.CommandContext;
import main.commands.CommandManager;
import main.commands.ICommand;
import main.commands.IDevCommand;
import main.constants.BotConstants;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.component.InteractionBuilderException;
import main.utils.component.InteractiveCommand;
import main.utils.database.mongodb.cache.BotInfoCache;
import main.utils.database.sqlite3.BotDB;
import main.utils.database.sqlite3.ServerDB;
import main.utils.json.guildconfig.GuildConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.List;

public class HelpCommand extends InteractiveCommand implements ICommand {
    private final Logger logger = LoggerFactory.getLogger(HelpCommand.class);
    private final String menuName = "menu:help";

    @Override
    public void initCommand() {
        setInteractionCommand(getCommand());
        upsertCommand();
    }

    @Override
    public void initCommand(Guild g) {
        setInteractionCommand(getCommand());
        upsertCommand(g);
    }

    public void initCommandWithoutUpsertion() {
        setInteractionCommand(getCommand());
    }

    private InteractionCommand getCommand() {
        return InteractionCommand.create()
                .setCommand(Command.of(
                        getName(),
                        "See all the commands the bot has to offer to you!",
                        List.of(CommandOption.of(
                                OptionType.STRING,
                                "command",
                                "View help for a specific command",
                                false
                        ))
                ))
                .addSelectionDialogue(SelectionDialogue.of(
                        menuName,
                        "Select an option",
                        Pair.of(1,1),
                        List.of(
                                Triple.of("Management Commands", "help:management", "💼"),
                                Triple.of("Music Commands", "help:music", "🎶"),
                                Triple.of("Miscellaneous Commands", "help:misc", "⚒️"),
                                Triple.of("Utility Commands", "help:utility", "❓")
                        ),
                        e -> {
                             if (e.getMessage().getMessageReference() == null)
                                 return true;

                            var msgID = e.getMessage().getMessageReference().getMessageIdLong();
                            Message msg = e.getJDA().getTextChannelById(e.getTextChannel().getIdLong())
                                    .retrieveMessageById(msgID).complete(); // BAD BAD BAD PREDICATE | TODO FIGURE OUT A NEW ONE!
                            return e.getUser().getIdLong() == msg.getAuthor().getIdLong();
                        }
                )).build();
    }


    @Override @SneakyThrows
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final List<String> args = ctx.getArgs();
        final String prefix = new GuildConfig().getPrefix(ctx.getGuild().getIdLong());

        GeneralUtils.setCustomEmbed(
                "Help Command",
                "Type \"" + prefix + "help <command>\" to get more help on a specific command."
        );

        CommandManager manager = new CommandManager(Robertify.getCommandWaiter());

        if (args.isEmpty()) {
            EmbedBuilder eb = EmbedUtils.embedMessage("""
                            Join our [support server](https://discord.gg/VbjmtfJDvU)!

                            *Select an option to view the commands I have to offer!*""")
                    .addField("💼 Management Commands", "*Configure and control how Robertify operates in this guild!*", true)
                    .addField("🎶 Music Commands", "*Play music and control songs. You can do so much with these commands to make your experience immersive.*", true)
                    .addBlankField(true)
                    .addField("⚒️ Miscellaneous Commands", "*Tired of playing music all the time? Well, here are some commands you can play around with!*", true)
                    .addField("❓ Utility Commands", "*Curious about the bot? These are the commands for you to explore!*", true)
                    .addBlankField(true);
            msg.replyEmbeds(eb.build()).queue(repliedMsg ->
                    {
                        try {
                            repliedMsg.editMessageComponents(
                                        ActionRow.of(getInteractionCommand().getSelectionMenu(menuName))
                                    ).queue();
                        } catch (InteractionBuilderException e) {
                            logger.error("[FATAL ERROR] An unexpected error occurred!", e);
                        }
                    }
            );

            GeneralUtils.setDefaultEmbed();
            return;
        } else if (args.get(0).equalsIgnoreCase("dev")) {
            if (!BotInfoCache.getInstance().isDeveloper(ctx.getAuthor().getIdLong())) {
                EmbedBuilder eb = EmbedUtils.embedMessage("Nothing found for: `"+args.get(0)+"`");
                msg.replyEmbeds(eb.build()).queue();
                GeneralUtils.setDefaultEmbed();
                return;
            }

            StringBuilder stringBuilder = new StringBuilder();
            for (ICommand cmd : manager.getDevCommands())
                stringBuilder.append("`").append(cmd.getName()).append("`, ");

            EmbedBuilder eb = EmbedUtils.embedMessage("**Developer Commands**\n\n" +
                    "**Prefix**: `" + prefix + "`");
            eb.addField("Commands", stringBuilder.toString(), false);
            msg.replyEmbeds(eb.build()).queue();

            GeneralUtils.setDefaultEmbed();
            return;
        }

        String search = args.get(0);
        ICommand command = manager.getCommand(search);

        if (command == null) {
            EmbedBuilder eb = EmbedUtils.embedMessage("Nothing found for: `"+search+"`");
            msg.replyEmbeds(eb.build()).queue();
            GeneralUtils.setDefaultEmbed();
            return;
        } else if (command instanceof IDevCommand) {
            if (!BotInfoCache.getInstance().isDeveloper(ctx.getAuthor().getIdLong())) {
                EmbedBuilder eb = EmbedUtils.embedMessage("Nothing found for: `"+search+"`");
                msg.replyEmbeds(eb.build()).queue();
                GeneralUtils.setDefaultEmbed();
                return;
            }
        }

        EmbedBuilder eb = EmbedUtils.embedMessage(command.getHelp(prefix));
        eb.setAuthor("Help Command ["+command.getName()+"]", null, BotConstants.ICON_URL.toString());
        msg.replyEmbeds(eb.build()).queue();

        GeneralUtils.setDefaultEmbed();
    }

    @Override @SneakyThrows
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(getName())) return;

        if (event.getOptions().isEmpty()) {
            final String prefix = new GuildConfig().getPrefix(event.getGuild().getIdLong());

            GeneralUtils.setCustomEmbed(
                    "Help Command",
                    "Type \"" + prefix + "help <command>\" to get more help on a specific command."
            );

            EmbedBuilder eb = EmbedUtils.embedMessage("*Select an option to view the commands I have to offer!");
            event.replyEmbeds(eb.build())
                    .addActionRow(getInteractionCommand().getSelectionMenu(menuName))
                    .setEphemeral(true).queue();
        } else {
            final CommandManager manager = new CommandManager(Robertify.getCommandWaiter());
            final String command = event.getOption("command").getAsString();
            event.replyEmbeds(searchCommand(manager, command, event.getGuild(), event.getUser()).build())
                    .setEphemeral(true).queue();
        }

        GeneralUtils.setDefaultEmbed();
    }

    @Override @SneakyThrows
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        if (!event.getComponentId().equals(menuName)) return;

        var optionSelected = event.getSelectedOptions();
        final String prefix = new GuildConfig().getPrefix(event.getGuild().getIdLong());

        if (!getSelectionDialogue(menuName).checkPermission(event)) {
            event.replyEmbeds(EmbedUtils.embedMessage("You can't interact with this menu!").build())
                    .setEphemeral(true).queue();
            return;
        }

        switch (optionSelected.get(0).getValue()) {
            case "help:management" -> {
                event.editMessageEmbeds(getHelpEmbed(HelpType.MANAGEMENT, prefix).build()).queue();
            }
            case "help:music" -> {
                event.editMessageEmbeds(getHelpEmbed(HelpType.MUSIC, prefix).build()).queue();
            }
            case "help:misc" -> {
                event.editMessageEmbeds(getHelpEmbed(HelpType.MISCELLANEOUS, prefix).build()).queue();
            }
            case "help:utility" -> {
                event.editMessageEmbeds(getHelpEmbed(HelpType.UTILITY, prefix).build()).queue();
            }
        }
    }

    @SneakyThrows
    private EmbedBuilder searchCommand(CommandManager manager, String search, Guild guild, User user) {
        ICommand command = manager.getCommand(search);

        if (command == null) {
            EmbedBuilder eb = EmbedUtils.embedMessage("Nothing found for: `"+search+"`");
            GeneralUtils.setDefaultEmbed();
            return eb;
        } else if (command instanceof IDevCommand) {
            if (!BotInfoCache.getInstance().isDeveloper(user.getIdLong())) {
                EmbedBuilder eb = EmbedUtils.embedMessage("Nothing found for: `"+search+"`");
                GeneralUtils.setDefaultEmbed();
                return eb;
            }
        }

        EmbedBuilder eb = EmbedUtils.embedMessage(command.getHelp(guild.getId()));
        eb.setAuthor("Help Command ["+command.getName()+"]", null, BotConstants.ICON_URL.toString());
        return eb;
    }

    private EmbedBuilder getHelpEmbed(HelpType type, String prefix) {
        EmbedBuilder eb = EmbedUtils.embedMessage("Here are your commands!\n\n" +
                "**Prefix**: `"+prefix+"`");
        CommandManager manager = new CommandManager(Robertify.getCommandWaiter());
        StringBuilder sb = new StringBuilder();

        switch (type) {
            case MANAGEMENT -> {
                List<ICommand> managementCommands = manager.getManagementCommands();
                for (ICommand cmd : managementCommands)
                    sb.append("`").append(cmd.getName()).append(
                            managementCommands.get(managementCommands.size()-1).equals(cmd) ?
                                    "`" : "`, "
                    );
                eb.addField("Management Commands", sb.toString(), false);
            }
            case MISCELLANEOUS -> {
                List<ICommand> miscCommands = manager.getMiscCommands();
                for (ICommand cmd : manager.getMiscCommands())
                    sb.append("`").append(cmd.getName()).append(
                            miscCommands.get(miscCommands.size()-1).equals(cmd) ?
                                    "`" : "`, "
                    );
                eb.addField("Miscellaneous Commands", sb.toString(), false);
            }
            case MUSIC -> {
                List<ICommand> musicCommands = manager.getMusicCommands();
                for (ICommand cmd : musicCommands)
                    sb.append("`").append(cmd.getName()).append(
                            musicCommands.get(musicCommands.size()-1).equals(cmd) ?
                                    "`" : "`, "
                    );
                eb.addField("Music Commands", sb.toString(), false);
            }
            case UTILITY -> {
                List<ICommand> utilityCommands = manager.getUtilityCommands();
                for (ICommand cmd : utilityCommands)
                    sb.append("`").append(cmd.getName()).append(
                            utilityCommands.get(utilityCommands.size()-1).equals(cmd) ?
                                    "`" : "`, "
                    );
                eb.addField("Utility Commands", sb.toString(), false);
            }
        }

        return eb;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    enum HelpType {
        MANAGEMENT,
        MUSIC,
        MISCELLANEOUS,
        UTILITY
    }
}
