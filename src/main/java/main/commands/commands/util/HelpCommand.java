package main.commands.commands.util;

import lombok.SneakyThrows;
import main.commands.CommandContext;
import main.commands.CommandManager;
import main.commands.ICommand;
import main.commands.IDevCommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.InteractionBuilderException;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.component.interactions.selectionmenu.SelectionMenuBuilder;
import main.utils.component.interactions.selectionmenu.SelectionMenuOption;
import main.utils.component.legacy.InteractiveCommand;
import main.utils.database.mongodb.cache.BotInfoCache;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.themes.ThemesConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.List;

public class HelpCommand extends AbstractSlashCommand implements ICommand {
    private final Logger logger = LoggerFactory.getLogger(HelpCommand.class);
    private final String menuName = "menu:help";

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("help")
                        .setDescription("See all the commands the bot has to offer to you!")
                        .addOptions(
                                CommandOption.of(
                                        OptionType.STRING,
                                        "command",
                                        "View help for a specific command",
                                        false
                                )
                        )
                        .build()
        );
    }

    private SelectionMenu getSelectionMenu(long userId) {
        return getSelectionMenuBuilder(userId).build();
    }

    private SelectionMenuBuilder getSelectionMenuBuilder(long userId) {
        return new SelectionMenuBuilder()
                .setName(menuName)
                .setPlaceHolder("Select an option!")
                .setRange(1, 1)
                .addOptions(
                        SelectionMenuOption.of("Management Commands", "help:management", Emoji.fromUnicode("💼")),
                        SelectionMenuOption.of("Music Commands", "help:music", Emoji.fromUnicode("🎶")),
                        SelectionMenuOption.of("Miscellaneous Commands", "help:misc", Emoji.fromUnicode("⚒️")),
                        SelectionMenuOption.of("Utility Commands", "help:utility", Emoji.fromUnicode("❓"))
                )
                .limitToUser(userId);
    }

    @Override
    public String getHelp() {
        return null;
    }


    @Override @SneakyThrows
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final List<String> args = ctx.getArgs();
        final var guild = ctx.getGuild();
        final String prefix = new GuildConfig().getPrefix(ctx.getGuild().getIdLong());

        GeneralUtils.setCustomEmbed(
                ctx.getGuild(),
                "Help Command",
                "Type \"" + prefix + "help <command>\" to get more help on a specific command."
        );

        CommandManager manager = new CommandManager();

        if (args.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, """
                            Join our [support server](https://discord.gg/VbjmtfJDvU)!

                            *Select an option to view the commands I have to offer!*""")
                    .addField("💼 Management Commands", "*Configure and control how Robertify operates in this guild!*", true)
                    .addField("🎶 Music Commands", "*Play music and control songs. You can do so much with these commands to make your experience immersive.*", true)
                    .addBlankField(true)
                    .addField("⚒️ Miscellaneous Commands", "*Tired of playing music all the time? Well, here are some commands you can play around with!*", true)
                    .addField("❓ Utility Commands", "*Curious about the bot? These are the commands for you to explore!*", true)
                    .addBlankField(true);
            msg.replyEmbeds(eb.build()).queue(repliedMsg ->
                    repliedMsg.editMessageComponents(
                            ActionRow.of(getSelectionMenu(ctx.getAuthor().getIdLong()))
                    ).queue()
            );
            GeneralUtils.setDefaultEmbed(ctx.getGuild());
            return;
        } else if (args.get(0).equalsIgnoreCase("dev")) {
            if (!BotInfoCache.getInstance().isDeveloper(ctx.getAuthor().getIdLong())) {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "Nothing found for: `"+args.get(0)+"`");
                msg.replyEmbeds(eb.build()).queue();
                GeneralUtils.setDefaultEmbed(ctx.getGuild());
                return;
            }

            StringBuilder stringBuilder = new StringBuilder();
            for (ICommand cmd : manager.getDevCommands())
                stringBuilder.append("`").append(cmd.getName()).append("`, ");

            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "**Developer Commands**\n\n" +
                    "**Prefix**: `" + prefix + "`");
            eb.addField("Commands", stringBuilder.toString(), false);
            msg.replyEmbeds(eb.build()).queue();

            GeneralUtils.setDefaultEmbed(ctx.getGuild());
            return;
        }

        String search = args.get(0);
        ICommand command = manager.getCommand(search);

        if (command == null) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "Nothing found for: `"+search+"`");
            msg.replyEmbeds(eb.build()).queue();
            GeneralUtils.setDefaultEmbed(ctx.getGuild());
            return;
        } else if (command instanceof IDevCommand) {
            if (!BotInfoCache.getInstance().isDeveloper(ctx.getAuthor().getIdLong())) {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "Nothing found for: `"+search+"`");
                msg.replyEmbeds(eb.build()).queue();
                GeneralUtils.setDefaultEmbed(ctx.getGuild());
                return;
            }
        }

        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, command.getHelp(prefix));
        final var theme = new ThemesConfig().getTheme(guild.getIdLong());
        eb.setAuthor("Help Command ["+command.getName()+"]", null, theme.getTransparent());
        msg.replyEmbeds(eb.build()).queue();

        GeneralUtils.setDefaultEmbed(ctx.getGuild());
    }

    @Override @SneakyThrows
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(getName())) return;

        if (event.getOptions().isEmpty()) {
            final String prefix = new GuildConfig().getPrefix(event.getGuild().getIdLong());

            GeneralUtils.setCustomEmbed(
                    event.getGuild(),
                    "Help Command",
                    "Type \"" + prefix + "help <command>\" to get more help on a specific command."
            );

            final var guild = event.getGuild();

            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, """
                            Join our [support server](https://discord.gg/VbjmtfJDvU)!

                            *Select an option to view the commands I have to offer!*""")
                    .addField("💼 Management Commands", "*Configure and control how Robertify operates in this guild!*", true)
                    .addField("🎶 Music Commands", "*Play music and control songs. You can do so much with these commands to make your experience immersive.*", true)
                    .addBlankField(true)
                    .addField("⚒️ Miscellaneous Commands", "*Tired of playing music all the time? Well, here are some commands you can play around with!*", true)
                    .addField("❓ Utility Commands", "*Curious about the bot? These are the commands for you to explore!*", true)
                    .addBlankField(true);
            event.replyEmbeds(eb.build())
                    .addActionRow(getSelectionMenu(event.getUser().getIdLong()))
                    .setEphemeral(true).queue();
        } else {
            final CommandManager manager = new CommandManager();
            final String command = event.getOption("command").getAsString();
            event.replyEmbeds(searchCommand(manager, command, event.getGuild(), event.getUser()).build())
                    .setEphemeral(true).queue();
        }

        GeneralUtils.setDefaultEmbed(event.getGuild());
    }

    @Override @SneakyThrows
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        if (!event.getComponentId().equals(menuName)) return;

        final var guild = event.getGuild();

        if (!getSelectionMenuBuilder(event.getUser().getIdLong()).checkPermission(event)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You can't interact with this menu!").build())
                    .setEphemeral(true).queue();
            return;
        }

        var optionSelected = event.getSelectedOptions();
        final String prefix = new GuildConfig().getPrefix(event.getGuild().getIdLong());

        switch (optionSelected.get(0).getValue()) {
            case "help:management" -> {
                event.editMessageEmbeds(getHelpEmbed(guild, HelpType.MANAGEMENT, prefix).build()).queue();
            }
            case "help:music" -> {
                event.editMessageEmbeds(getHelpEmbed(guild, HelpType.MUSIC, prefix).build()).queue();
            }
            case "help:misc" -> {
                event.editMessageEmbeds(getHelpEmbed(guild, HelpType.MISCELLANEOUS, prefix).build()).queue();
            }
            case "help:utility" -> {
                event.editMessageEmbeds(getHelpEmbed(guild, HelpType.UTILITY, prefix).build()).queue();
            }
        }
    }

    @SneakyThrows
    private EmbedBuilder searchCommand(CommandManager manager, String search, Guild guild, User user) {
        final ICommand command = manager.getCommand(search);

        if (command == null) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "Nothing found for: `"+search+"`");
            GeneralUtils.setDefaultEmbed(guild);
            return eb;
        } else if (command instanceof IDevCommand) {
            if (!BotInfoCache.getInstance().isDeveloper(user.getIdLong())) {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "Nothing found for: `"+search+"`");
                GeneralUtils.setDefaultEmbed(guild);
                return eb;
            }
        }

        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, command.getHelp(guild.getId()));
        final var theme = new ThemesConfig().getTheme(guild.getIdLong());
        eb.setAuthor("Help Command ["+command.getName()+"]", null, theme.getTransparent());
        return eb;
    }

    private EmbedBuilder getHelpEmbed(Guild guild, HelpType type, String prefix) {
        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "Here are your commands!\n\n" +
                "**Prefix**: `"+prefix+"`");
        CommandManager manager = new CommandManager();
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
