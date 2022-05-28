package main.commands.slashcommands.commands.management;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.CommandManager;
import main.commands.prefixcommands.ICommand;
import main.commands.slashcommands.SlashCommandManager;
import main.constants.Permission;
import main.constants.RobertifyEmoji;
import main.constants.Toggles;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.logs.LogType;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.List;

public class TogglesCommand extends AbstractSlashCommand implements ICommand {
    private final Logger logger = LoggerFactory.getLogger(TogglesCommand.class);

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final Guild guild = ctx.getGuild();

        if (!GeneralUtils.hasPerms(guild, ctx.getMember(), Permission.ROBERTIFY_ADMIN))
            return;

        GeneralUtils.setCustomEmbed(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLES_EMBED_TITLE);

        final var localeManager = LocaleManager.getLocaleManager(guild);
        var config = new TogglesConfig();
        if (args.isEmpty()) {
            var toggleIDs = new StringBuilder();
            var toggleNames = new StringBuilder();
            var toggleStatuses = new StringBuilder();

            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "\t");

            int toggleID = 0;
            for (Toggles toggle : Toggles.values()) {
                toggleIDs.append(++toggleID).append("\n");
                toggleNames.append(Toggles.parseToggle(toggle)).append("\n");
                toggleStatuses.append(config.getToggle(guild, toggle) ? RobertifyEmoji.CHECK_EMOJI.toString() : RobertifyEmoji.QUIT_EMOJI.toString())
                        .append("\n");
            }

            eb.addField(localeManager.getMessage(RobertifyLocaleMessage.TogglesMessages.TOGGLES_EMBED_TOGGLE_ID_FIELD), toggleIDs.toString(), true);
            eb.addField(localeManager.getMessage(RobertifyLocaleMessage.TogglesMessages.TOGGLES_MESSAGES_EMBED_FEATURE_FIELD), toggleNames.toString(), true);
            eb.addField(localeManager.getMessage(RobertifyLocaleMessage.TogglesMessages.TOGGLES_EMBED_STATUS_FIELD), toggleStatuses.toString(), true);

            msg.replyEmbeds(eb.build()).queue();
        } else {
            var eb = new EmbedBuilder();
            switch (args.get(0).toLowerCase()) {
                case "restrictedvoice", "1", "rvc", "rvchannels" -> {
                    if (config.getToggle(guild, Toggles.RESTRICTED_VOICE_CHANNELS)) {
                        config.setToggle(guild, Toggles.RESTRICTED_VOICE_CHANNELS, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "`Restricted Voice Channels`"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        );
                    } else {
                        config.setToggle(guild, Toggles.RESTRICTED_VOICE_CHANNELS, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "`Restricted Voice Channels`"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                        );
                    }
                }
                case "restrictedtext", "2", "rtc", "rtchannels" -> {
                    if (config.getToggle(guild, Toggles.RESTRICTED_TEXT_CHANNELS)) {
                        config.setToggle(guild, Toggles.RESTRICTED_TEXT_CHANNELS, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "`Restricted Text Channels`"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        );
                    } else {
                        config.setToggle(guild, Toggles.RESTRICTED_TEXT_CHANNELS, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "`Restricted Text Channels`"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                        );
                    }
                }
                case "announcements", "3" -> {
                    if (config.getToggle(guild, Toggles.ANNOUNCE_MESSAGES)) {
                        config.setToggle(guild, Toggles.ANNOUNCE_MESSAGES, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "`Announcing Player Messages`"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        );
                    } else {
                        config.setToggle(guild, Toggles.ANNOUNCE_MESSAGES, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "`Announcing Player Messages`"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                        );
                    }
                }
                case "requester", "4" -> {
                    if (config.getToggle(guild, Toggles.SHOW_REQUESTER)) {
                        config.setToggle(guild, Toggles.SHOW_REQUESTER, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "showing the requester in now playing messages"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        );
                    } else {
                        config.setToggle(guild, Toggles.SHOW_REQUESTER, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "showing the requester in now playing messages"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                        );
                    }
                }
                case "8ball", "5" -> {
                    if (config.getToggle(guild, Toggles.EIGHT_BALL)) {
                        config.setToggle(guild, Toggles.EIGHT_BALL, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "8ball command"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        );
                    } else {
                        config.setToggle(guild, Toggles.EIGHT_BALL, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "8ball command"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                        );
                    }
                }
                case "polls", "poll", "6" -> {
                    if (config.getToggle(guild, Toggles.POLLS)) {
                        config.setToggle(guild, Toggles.POLLS, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "polls command"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        );
                    } else {
                        config.setToggle(guild, Toggles.POLLS, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "polls command"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                        );
                    }
                }
                case "tips", "7" -> {
                    if (config.getToggle(guild, Toggles.TIPS)) {
                        config.setToggle(guild, Toggles.TIPS, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "tips"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        );
                    } else {
                        config.setToggle(guild, Toggles.TIPS, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "tips"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                        );
                    }
                }
                case "voteskips", "voteskip", "vs", "8" -> {
                    if (config.getToggle(guild, Toggles.VOTE_SKIPS)) {
                        config.setToggle(guild, Toggles.VOTE_SKIPS, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "vote skips"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        );
                    } else {
                        config.setToggle(guild, Toggles.VOTE_SKIPS, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "vote skips"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                        );
                    }
                }
                case "dj" -> eb = handleDJToggles(guild, args);
                case "logs", "log", "l" -> eb = handleLogToggles(guild, args);
                default -> eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.INVALID_TOGGLE);
            }
            msg.replyEmbeds(eb.build()).queue();
        }

        GeneralUtils.setDefaultEmbed(guild);
    }

    private EmbedBuilder handleDJToggles(Guild guild, List<String> args) {
        final var commandManager = new SlashCommandManager();
        final TogglesConfig config = new TogglesConfig();

        if (args.size() < 2)
            return getDJTogglesEmbed(guild, commandManager, config);

        switch (args.get(1).toLowerCase()) {
            case "list" -> {
                return getDJTogglesEmbed(guild, commandManager, config);
            }
            default -> {
                var command = commandManager.getCommand(args.get(1));

                if (command == null)
                    return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.DJ_TOGGLE_INVALID_COMMAND, Pair.of("{command}", args.get(1)));

                final var localeManager = LocaleManager.getLocaleManager(guild);

                switch (Boolean.toString(config.getDJToggle(guild, command))) {
                    case "true" -> {
                        config.setDJToggle(guild, command, false);
                        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.DJ_TOGGLED,
                                Pair.of("{command}", command.getName()),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        );
                    }
                    case "false" -> {
                        config.setDJToggle(guild, command, true);
                        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.DJ_TOGGLED,
                                Pair.of("{command}", command.getName()),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                        );
                    }
                    default -> logger.error("Did not receive either true or false. Lol?? How??");
                }
            }
        }
        return null;
    }

    private EmbedBuilder handleLogToggles(Guild guild, List<String> args) {
        final CommandManager commandManager = new CommandManager();
        final TogglesConfig config = new TogglesConfig();

        if (args.size() < 2)
            return getLogTogglesEmbed(guild, config);

        switch (args.get(1).toLowerCase()) {
            case "list" -> {
                return getLogTogglesEmbed(guild, config);
            }
            default -> {
                LogType logType;
                try {
                    logType = LogType.valueOf(args.get(1).toUpperCase());
                } catch (IllegalArgumentException e) {
                    return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.LOG_TOGGLE_INVALID_TYPE, Pair.of("{command}", args.get(1)));
                }

                final var localeManager = LocaleManager.getLocaleManager(guild);

                switch (Boolean.toString(config.getLogToggle(guild, logType))) {
                    case "true" -> {
                        config.setLogToggle(guild, logType, false);
                        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.LOG_TOGGLED,
                                Pair.of("{logType}", logType.getName()),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        );
                    }
                    case "false" -> {
                        config.setLogToggle(guild, logType, true);
                        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.LOG_TOGGLED,
                                Pair.of("{logType}", logType.getName()),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                        );
                    }
                    default -> logger.error("Did not receive either true or false. Lol?? How??");
                }
            }
        }
        return null;
    }

    private EmbedBuilder getDJTogglesEmbed(Guild guild, SlashCommandManager commandManager, TogglesConfig config) {
        var musicCmds = commandManager.getMusicCommands();
        var toggleNames = new StringBuilder();
        var toggleStatuses = new StringBuilder();

        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "\t");

        for (AbstractSlashCommand toggle : musicCmds) {
            toggleNames.append(toggle.getName()).append("\n");
            toggleStatuses.append(config.getDJToggle(guild, toggle) ? RobertifyEmoji.CHECK_EMOJI.toString() : RobertifyEmoji.QUIT_EMOJI.toString())
                    .append("\n");
        }

        final var localeManager = LocaleManager.getLocaleManager(guild);
        eb.addField(localeManager.getMessage(RobertifyLocaleMessage.TogglesMessages.DJ_TOGGLES_EMBED_COMMAND_FIELD), toggleNames.toString(), true);
        eb.addBlankField(true);
        eb.addField(localeManager.getMessage(RobertifyLocaleMessage.TogglesMessages.DJ_TOGGLES_EMBED_STATUS_FIELD), toggleStatuses.toString(), true);

        return eb;
    }

    private EmbedBuilder getLogTogglesEmbed(Guild guild, TogglesConfig config) {
        var logTypes = LogType.values();
        var toggleNames = new StringBuilder();
        var toggleStatuses = new StringBuilder();

        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "\t");

        for (var toggle : logTypes) {
            toggleNames.append(toggle.name().toLowerCase()).append("\n");
            toggleStatuses.append(config.getLogToggle(guild, toggle) ? RobertifyEmoji.CHECK_EMOJI.toString() : RobertifyEmoji.QUIT_EMOJI.toString())
                    .append("\n");
        }

        final var localeManager = LocaleManager.getLocaleManager(guild);
        eb.addField(localeManager.getMessage(RobertifyLocaleMessage.TogglesMessages.LOG_TOGGLES_EMBED_TYPE_FIELD), toggleNames.toString(), true);
        eb.addBlankField(true);
        eb.addField(localeManager.getMessage(RobertifyLocaleMessage.TogglesMessages.LOG_TOGGLES_EMBED_STATUS_FIELD), toggleStatuses.toString(), true);

        return eb;
    }

    @Override
    public String getName() {
        return "toggles";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`\n" +
                "Toggle specific features on or off!\n\n**__Usages__**\n" +
                "`"+ prefix +"toggles`\n" +
                "`"+ prefix +"toggles <toggle_name>`\n" +
                 "`"+ prefix +"toggles dj <list|command>`\n";
    }

    @Override
    public List<String> getAliases() {
        return List.of("t", "tog", "toggle");
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("toggles")
                        .setDescription("Toggle specific features on or off!")
                        .addSubCommands(
                                SubCommand.of(
                                        "list",
                                        "List all toggles and their statuses!"
                                ),
                                SubCommand.of(
                                        "switch",
                                        "Turn a specific toggle on or off.",
                                        List.of(
                                                CommandOption.of(
                                                        OptionType.STRING,
                                                        "toggle",
                                                        "The toggle to switch",
                                                        true,
                                                        Toggles.toList()
                                                )
                                        )
                                )
                        )
                        .addSubCommandGroups(
                                SubCommandGroup.of(
                                        "dj",
                                        "Configure DJ toggles for the bot!",
                                        List.of(
                                                SubCommand.of(
                                                        "list",
                                                        "List all DJ toggles!"
                                                ),
                                                SubCommand.of(
                                                        "switch",
                                                        "Switch a specific DJ toggle",
                                                        List.of(
                                                                CommandOption.of(
                                                                        OptionType.STRING,
                                                                        "toggle",
                                                                        "The DJ toggle to switch",
                                                                        true
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                SubCommandGroup.of(
                                        "logs",
                                        "Configure log toggles for the bot!",
                                        List.of(
                                                SubCommand.of(
                                                        "list",
                                                        "List all log toggles!"
                                                ),
                                                SubCommand.of(
                                                        "switch",
                                                        "Switch a specific log toggle",
                                                        List.of(
                                                                CommandOption.of(
                                                                        OptionType.STRING,
                                                                        "toggle",
                                                                        "The log toggle to switch",
                                                                        true,
                                                                        LogType.toList()
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .setAdminOnly()
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

        final var guild = event.getGuild();
        final var config = new TogglesConfig();
        final var path = event.getCommandPath().split("/");
        final var localeManager = LocaleManager.getLocaleManager(guild);
        EmbedBuilder eb = null;

        switch (path[1]) {
            case "list" -> {
                var toggleIDs = new StringBuilder();
                var toggleNames = new StringBuilder();
                var toggleStatuses = new StringBuilder();

                eb = RobertifyEmbedUtils.embedMessage(guild, "\t");

                int toggleID = 0;
                for (Toggles toggle : Toggles.values()) {
                    toggleIDs.append(++toggleID).append("\n");
                    toggleNames.append(Toggles.parseToggle(toggle)).append("\n");
                    toggleStatuses.append(config.getToggle(guild, toggle) ? RobertifyEmoji.CHECK_EMOJI.toString() : RobertifyEmoji.QUIT_EMOJI.toString())
                            .append("\n");
                }

                eb.addField(localeManager.getMessage(RobertifyLocaleMessage.TogglesMessages.TOGGLES_EMBED_TOGGLE_ID_FIELD), toggleIDs.toString(), true);
                eb.addField(localeManager.getMessage(RobertifyLocaleMessage.TogglesMessages.TOGGLES_MESSAGES_EMBED_FEATURE_FIELD), toggleNames.toString(), true);
                eb.addField(localeManager.getMessage(RobertifyLocaleMessage.TogglesMessages.TOGGLES_EMBED_STATUS_FIELD), toggleStatuses.toString(), true);
            }
            case "switch" -> {
                final var toggle = event.getOption("toggle").getAsString();

                switch (toggle.toLowerCase()) {
                    case "restrictedvoice", "1", "rvc", "rvchannels" -> {
                        if (config.getToggle(guild, Toggles.RESTRICTED_VOICE_CHANNELS)) {
                            config.setToggle(guild, Toggles.RESTRICTED_VOICE_CHANNELS, false);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "`Restricted Voice Channels`"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                            );
                        } else {
                            config.setToggle(guild, Toggles.RESTRICTED_VOICE_CHANNELS, true);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "`Restricted Voice Channels`"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                            );
                        }
                    }
                    case "restrictedtext", "2", "rtc", "rtchannels" -> {
                        if (config.getToggle(guild, Toggles.RESTRICTED_TEXT_CHANNELS)) {
                            config.setToggle(guild, Toggles.RESTRICTED_TEXT_CHANNELS, false);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "`Restricted Text Channels`"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                            );
                        } else {
                            config.setToggle(guild, Toggles.RESTRICTED_TEXT_CHANNELS, true);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "`Restricted Text Channels`"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                            );
                        }
                    }
                    case "announcements", "3" -> {
                        if (config.getToggle(guild, Toggles.ANNOUNCE_MESSAGES)) {
                            config.setToggle(guild, Toggles.ANNOUNCE_MESSAGES, false);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "`Announcing Player Messages`"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                            );
                        } else {
                            config.setToggle(guild, Toggles.ANNOUNCE_MESSAGES, true);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "`Announcing Player Messages`"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                            );
                        }
                    }
                    case "requester", "4" -> {
                        if (config.getToggle(guild, Toggles.SHOW_REQUESTER)) {
                            config.setToggle(guild, Toggles.SHOW_REQUESTER, false);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "showing the requester in now playing messages"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                            );
                        } else {
                            config.setToggle(guild, Toggles.SHOW_REQUESTER, true);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "showing the requester in now playing messages"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                            );
                        }
                    }
                    case "8ball", "5" -> {
                        if (config.getToggle(guild, Toggles.EIGHT_BALL)) {
                            config.setToggle(guild, Toggles.EIGHT_BALL, false);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "8ball command"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                            );
                        } else {
                            config.setToggle(guild, Toggles.EIGHT_BALL, true);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "8ball command"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                            );
                        }
                    }
                    case "polls", "poll", "6" -> {
                        if (config.getToggle(guild, Toggles.POLLS)) {
                            config.setToggle(guild, Toggles.POLLS, false);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "polls command"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                            );
                        } else {
                            config.setToggle(guild, Toggles.POLLS, true);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "polls command"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                            );
                        }
                    }
                    case "tips", "7" -> {
                        if (config.getToggle(guild, Toggles.TIPS)) {
                            config.setToggle(guild, Toggles.TIPS, false);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "tips"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                            );
                        } else {
                            config.setToggle(guild, Toggles.TIPS, true);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "tips"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                            );
                        }
                    }
                    case "voteskips", "voteskip", "vs", "8" -> {
                        if (config.getToggle(guild, Toggles.VOTE_SKIPS)) {
                            config.setToggle(guild, Toggles.VOTE_SKIPS, false);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "vote skips"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                            );
                        } else {
                            config.setToggle(guild, Toggles.VOTE_SKIPS, true);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "vote skips"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                            );
                        }
                    }
                    default -> eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.INVALID_TOGGLE);
                }
            }
            case "dj" -> {
                switch (path[2]) {
                    case "list" -> eb = getDJTogglesEmbed(guild, new SlashCommandManager(), config);
                    case "switch" -> {
                        final String toggle = event.getOption("toggle").getAsString();
                        AbstractSlashCommand command = new SlashCommandManager().getCommand(toggle);

                        if (command == null) {
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.DJ_TOGGLE_INVALID_COMMAND, Pair.of("{command}", toggle));
                            break;
                        }

                        if (config.getDJToggle(guild, command)) {
                            config.setDJToggle(guild, command, false);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.DJ_TOGGLED,
                                    Pair.of("{command}", command.getName()),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                            );
                        } else {
                            config.setDJToggle(guild, command, true);
                            RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.DJ_TOGGLED,
                                    Pair.of("{command}", command.getName()),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                            );
                        }
                    }
                }
            }
            case "logs" -> {
                switch (path[2]) {
                    case "list" -> eb = getLogTogglesEmbed(guild, config);
                    case "switch" -> {
                        final var logTypeStr = event.getOption("toggle").getAsString();
                        LogType logType;
                        try {
                            logType = LogType.valueOf(logTypeStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            eb =  RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.LOG_TOGGLE_INVALID_TYPE, Pair.of("{logType}", logTypeStr));
                            break;
                        }

                        switch (Boolean.toString(config.getLogToggle(guild, logType))) {
                            case "true" -> {
                                config.setLogToggle(guild, logType, false);
                                eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.LOG_TOGGLED,
                                        Pair.of("{logType}", logType.getName()),
                                        Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                                );
                            }
                            case "false" -> {
                                config.setLogToggle(guild, logType, true);
                                eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.LOG_TOGGLED,
                                        Pair.of("{logType}", logType.getName()),
                                        Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                                );
                            }
                            default -> logger.error("Did not receive either true or false. Lol?? How??");
                        }
                    }
                }
            }
        }
        event.replyEmbeds(eb.build()).queue();
    }
}
