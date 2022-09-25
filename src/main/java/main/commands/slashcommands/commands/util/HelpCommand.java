package main.commands.slashcommands.commands.util;

import lombok.SneakyThrows;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.commands.slashcommands.SlashCommandManager;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.component.interactions.selectionmenu.SelectMenuOption;
import main.utils.component.interactions.selectionmenu.SelectionMenuBuilder;
import main.utils.database.mongodb.cache.BotBDCache;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.themes.ThemesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.internal.utils.tuple.Pair;
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

    private SelectMenu getSelectionMenu(Guild guild, long userId) {
        return getSelectionMenuBuilder(guild, userId).build();
    }

    private SelectionMenuBuilder getSelectionMenuBuilder(Guild guild, long userId) {
        final var localeManager = LocaleManager.getLocaleManager(guild);
        return new SelectionMenuBuilder()
                .setName(menuName)
            .setPlaceHolder(localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.SELECT_MENU_PLACEHOLDER))
                .setRange(1, 1)
                .addOptions(
                        SelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_MANAGEMENT_OPTION), "help:management", Emoji.fromUnicode("💼")),
                        SelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_MUSIC_OPTION), "help:music", Emoji.fromUnicode("🎶")),
                        SelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_MISCELLANEOUS_OPTION), "help:misc", Emoji.fromUnicode("⚒️")),
                        SelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_UTILITY_OPTION), "help:utility", Emoji.fromUnicode("❓"))
                )
                .limitToUser(userId);
    }

    @Override
    public String getHelp() {
        return null;
    }


    @Override @SneakyThrows
    public void handle(CommandContext ctx) throws ScriptException {
        final var msg = ctx.getMessage();
        final var args = ctx.getArgs();
        final var guild = ctx.getGuild();
        final var localeManager = LocaleManager.getLocaleManager(guild);
        final var prefix = new GuildConfig(guild).getPrefix();

        GeneralUtils.setCustomEmbed(
                ctx.getGuild(),
                localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_EMBED_AUTHOR),
                localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_EMBED_FOOTER)
        );

        final var manager = new SlashCommandManager();

        if (args.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.HelpMessages.HELP_EMBED_DESC)
                    .addField("💼 " + localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_MANAGEMENT_OPTION), localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_MANAGEMENT_OPTION_DESC), true)
                    .addField("🎶 "+ localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_MUSIC_OPTION), localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_MUSIC_OPTION_DESC), true)
                    .addBlankField(true)
                    .addField("⚒️ "+ localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_MISCELLANEOUS_OPTION), localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_MISCELLANEOUS_OPTION_DESC), true)
                    .addField("❓ "+ localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_UTILITY_OPTION), localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_UTILITY_OPTION_DESC), true)
                    .addBlankField(true);
            msg.replyEmbeds(eb.build()).queue(repliedMsg ->
                    repliedMsg.editMessageComponents(
                            ActionRow.of(getSelectionMenu(guild, ctx.getAuthor().getIdLong()))
                    ).queue()
            );
            GeneralUtils.setDefaultEmbed(ctx.getGuild());
            return;
        } else if (args.get(0).equalsIgnoreCase("dev")) {
            if (!BotBDCache.getInstance().isDeveloper(ctx.getAuthor().getIdLong())) {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.HelpMessages.HELP_NOTHING_FOUND, Pair.of("{command}", args.get(0)));
                msg.replyEmbeds(eb.build()).queue();
                GeneralUtils.setDefaultEmbed(ctx.getGuild());
                return;
            }

            StringBuilder stringBuilder = new StringBuilder();
            for (var cmd : manager.getDevCommands())
                stringBuilder.append("`").append(cmd.getName()).append("`, ");

            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "**Developer Commands**\n\n" +
                    "**Prefix**: `" + prefix + "`");
            eb.addField("Commands", stringBuilder.toString(), false);
            msg.replyEmbeds(eb.build()).queue();

            GeneralUtils.setDefaultEmbed(ctx.getGuild());
            return;
        }

        final var search = args.get(0);
        final var command = manager.getCommand(search);

        if (command == null) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.HelpMessages.HELP_NOTHING_FOUND, Pair.of("{command}", search));
            msg.replyEmbeds(eb.build()).queue();
            GeneralUtils.setDefaultEmbed(ctx.getGuild());
            return;
        } else if (command.isDevCommand()) {
            if (!BotBDCache.getInstance().isDeveloper(ctx.getAuthor().getIdLong())) {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.HelpMessages.HELP_NOTHING_FOUND, Pair.of("{command}", search));
                msg.replyEmbeds(eb.build()).queue();
                GeneralUtils.setDefaultEmbed(ctx.getGuild());
                return;
            }
        }

        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, command.getHelp());
        final var theme = new ThemesConfig(guild).getTheme();
        eb.setAuthor(localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_EMBED_AUTHOR) + " ["+command.getName()+"]", null, theme.getTransparent());
        msg.replyEmbeds(eb.build()).queue();

        GeneralUtils.setDefaultEmbed(ctx.getGuild());
    }

    @Override @SneakyThrows
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;

        final var guild = event.getGuild();
        if (event.getOptions().isEmpty()) {
            final var localeManager = LocaleManager.getLocaleManager(guild);

            GeneralUtils.setCustomEmbed(
                    guild,
                    localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_EMBED_AUTHOR),
                    localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_EMBED_FOOTER)
            );

            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.HelpMessages.HELP_EMBED_DESC)
                    .addField("💼 " + localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_MANAGEMENT_OPTION), localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_MANAGEMENT_OPTION_DESC), true)
                    .addField("🎶 "+ localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_MUSIC_OPTION), localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_MUSIC_OPTION_DESC), true)
                    .addBlankField(true)
                    .addField("⚒️ "+ localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_MISCELLANEOUS_OPTION), localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_MISCELLANEOUS_OPTION_DESC), true)
                    .addField("❓ "+ localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_UTILITY_OPTION), localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_UTILITY_OPTION_DESC), true)
                    .addBlankField(true);
            event.replyEmbeds(eb.build())
                    .addActionRow(getSelectionMenu(guild, event.getUser().getIdLong()))
                    .setEphemeral(true).queue();
        } else {
            final var manager = new SlashCommandManager();
            final String command = event.getOption("command").getAsString();
            event.replyEmbeds(searchCommand(manager, command, guild).build())
                    .setEphemeral(true).queue();
        }

        GeneralUtils.setDefaultEmbed(guild);
    }

    @Override @SneakyThrows
    public void onSelectMenuInteraction(@NotNull SelectMenuInteractionEvent event) {
        if (!event.getComponentId().startsWith(menuName)) return;

        final var guild = event.getGuild();

        if (!event.getComponentId().split(":")[2].equals(event.getUser().getId())) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NO_MENU_PERMS).build())
                    .setEphemeral(true).queue();
            return;
        }

        var optionSelected = event.getSelectedOptions();
        final String prefix = new GuildConfig(guild).getPrefix();

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
    private EmbedBuilder searchCommand(SlashCommandManager manager, String search, Guild guild) {
        final var command = manager.getCommand(search);

        if (command == null) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.HelpMessages.HELP_NOTHING_FOUND, Pair.of("{command}", search));
            GeneralUtils.setDefaultEmbed(guild);
            return eb;
        }

        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, command.getHelp());
        final var theme = new ThemesConfig(guild).getTheme();
        final var localeManager = LocaleManager.getLocaleManager(guild);
        eb.setAuthor(localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_EMBED_AUTHOR) + " ["+command.getName()+"]", null, theme.getTransparent());
        return eb;
    }

    private EmbedBuilder getHelpEmbed(Guild guild, HelpType type, String prefix) {
        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.HelpMessages.HELP_COMMANDS, Pair.of("{prefix}", prefix));
        final var manager = new SlashCommandManager();
        final var localeManager = LocaleManager.getLocaleManager(guild);
        StringBuilder sb = new StringBuilder();

        switch (type) {
            case MANAGEMENT -> {
                List<AbstractSlashCommand> managementCommands = manager.getManagementCommands();
                for (var cmd : managementCommands)
                    sb.append("`").append(cmd.getName()).append(
                            managementCommands.get(managementCommands.size()-1).equals(cmd) ?
                                    "`" : "`, "
                    );
                eb.addField(localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_MANAGEMENT_OPTION), sb.toString(), false);
            }
            case MISCELLANEOUS -> {
                List<AbstractSlashCommand> miscCommands = manager.getMiscCommands();
                for (var cmd : manager.getMiscCommands())
                    sb.append("`").append(cmd.getName()).append(
                            miscCommands.get(miscCommands.size()-1).equals(cmd) ?
                                    "`" : "`, "
                    );
                eb.addField(localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_MISCELLANEOUS_OPTION), sb.toString(), false);
            }
            case MUSIC -> {
                List<AbstractSlashCommand> musicCommands = manager.getMusicCommands();
                for (var cmd : musicCommands)
                    sb.append("`").append(cmd.getName()).append(
                            musicCommands.get(musicCommands.size()-1).equals(cmd) ?
                                    "`" : "`, "
                    );
                eb.addField(localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_MUSIC_OPTION), sb.toString(), false);
            }
            case UTILITY -> {
                List<AbstractSlashCommand> utilityCommands = manager.getUtilityCommands();
                for (var cmd : utilityCommands)
                    sb.append("`").append(cmd.getName()).append(
                            utilityCommands.get(utilityCommands.size()-1).equals(cmd) ?
                                    "`" : "`, "
                    );
                eb.addField(localeManager.getMessage(RobertifyLocaleMessage.HelpMessages.HELP_UTILITY_OPTION), sb.toString(), false);
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
        return "Get help for all the command Robertify has to offer!";
    }

    enum HelpType {
        MANAGEMENT,
        MUSIC,
        MISCELLANEOUS,
        UTILITY
    }
}
