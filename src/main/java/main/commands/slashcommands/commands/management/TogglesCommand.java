package main.commands.slashcommands.commands.management;

import main.commands.prefixcommands.CommandContext;
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
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
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

        final var localeManager = LocaleManager.getLocaleManager(guild);
        var config = TogglesConfig.getConfig(guild);
        if (args.isEmpty()) {
            var toggleIDs = new StringBuilder();
            var toggleNames = new StringBuilder();
            var toggleStatuses = new StringBuilder();

            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "\t");

            int toggleID = 0;
            for (Toggles toggle : Toggles.values()) {
                toggleIDs.append(++toggleID).append("\n");
                toggleNames.append(Toggles.parseToggle(toggle)).append("\n");
                toggleStatuses.append(config.getToggle(toggle) ? RobertifyEmoji.CHECK_EMOJI.toString() : RobertifyEmoji.QUIT_EMOJI.toString())
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
                    if (config.getToggle(Toggles.RESTRICTED_VOICE_CHANNELS)) {
                        config.setToggle(Toggles.RESTRICTED_VOICE_CHANNELS, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "`Restricted Voice Channels`"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        );
                    } else {
                        config.setToggle(Toggles.RESTRICTED_VOICE_CHANNELS, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "`Restricted Voice Channels`"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                        );
                    }
                }
                case "restrictedtext", "2", "rtc", "rtchannels" -> {
                    if (config.getToggle(Toggles.RESTRICTED_TEXT_CHANNELS)) {
                        config.setToggle(Toggles.RESTRICTED_TEXT_CHANNELS, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "`Restricted Text Channels`"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        );
                    } else {
                        config.setToggle(Toggles.RESTRICTED_TEXT_CHANNELS, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "`Restricted Text Channels`"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                        );
                    }
                }
                case "announcements", "3" -> {
                    if (config.getToggle(Toggles.ANNOUNCE_MESSAGES)) {
                        config.setToggle(Toggles.ANNOUNCE_MESSAGES, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "`Announcing Player Messages`"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        );
                    } else {
                        config.setToggle(Toggles.ANNOUNCE_MESSAGES, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "`Announcing Player Messages`"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                        );
                    }
                }
                case "requester", "4" -> {
                    if (config.getToggle(Toggles.SHOW_REQUESTER)) {
                        config.setToggle(Toggles.SHOW_REQUESTER, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "showing the requester in now playing messages"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        );
                    } else {
                        config.setToggle(Toggles.SHOW_REQUESTER, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "showing the requester in now playing messages"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                        );
                    }
                }
                case "8ball", "5" -> {
                    if (config.getToggle(Toggles.EIGHT_BALL)) {
                        config.setToggle(Toggles.EIGHT_BALL, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "8ball command"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        );
                    } else {
                        config.setToggle(Toggles.EIGHT_BALL, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "8ball command"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                        );
                    }
                }
                case "polls", "poll", "6" -> {
                    if (config.getToggle(Toggles.POLLS)) {
                        config.setToggle(Toggles.POLLS, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "polls command"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        );
                    } else {
                        config.setToggle(Toggles.POLLS, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "polls command"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                        );
                    }
                }
                case "reminders", "7" -> {
                    if (config.getToggle(Toggles.REMINDERS)) {
                        config.setToggle(Toggles.REMINDERS, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "reminders"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        );
                    } else {
                        config.setToggle(Toggles.REMINDERS, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "reminders"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                        );
                    }
                }
                case "tips", "8" -> {
                    if (config.getToggle(Toggles.TIPS)) {
                        config.setToggle(Toggles.TIPS, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "tips"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        );
                    } else {
                        config.setToggle(Toggles.TIPS, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "tips"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                        );
                    }
                }
                case "voteskips", "voteskip", "vs", "9" -> {
                    if (config.getToggle(Toggles.VOTE_SKIPS)) {
                        config.setToggle(Toggles.VOTE_SKIPS, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "vote skips"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        );

                        final var skipCommand = SlashCommandManager.getInstance().getCommand("skip");
                        if (config.getDJToggle(skipCommand)) {
                            ctx.getChannel().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.SKIP_DJ_TOGGLE_PROMPT).build())
                                    .setActionRow(
                                            Button.of(ButtonStyle.SUCCESS, "toggledjskip:yes:" + ctx.getAuthor().getId(), "", RobertifyEmoji.CHECK_EMOJI.getEmoji()),
                                            Button.of(ButtonStyle.SECONDARY, "toggledjskip:no:" + ctx.getAuthor().getId(), "", RobertifyEmoji.QUIT_EMOJI.getEmoji())
                                    )
                                    .queue();
                        }
                    } else {
                        config.setToggle(Toggles.VOTE_SKIPS, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                Pair.of("{toggle}", "vote skips"),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                        );

                        final var skipCommand = SlashCommandManager.getInstance().getCommand("skip");
                        if (!config.getDJToggle(skipCommand))
                            config.setDJToggle(skipCommand, true);
                    }
                }
                case "dj" -> eb = handleDJToggles(guild, args);
                case "logs", "log", "l" -> eb = handleLogToggles(guild, args);
                default -> eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.INVALID_TOGGLE);
            }
            msg.replyEmbeds(eb.build()).queue();
        }
    }

    private EmbedBuilder handleDJToggles(Guild guild, List<String> args) {
        final var commandManager = SlashCommandManager.getInstance();
        final TogglesConfig config = TogglesConfig.getConfig(guild);

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

                switch (Boolean.toString(config.getDJToggle(command))) {
                    case "true" -> {
                        config.setDJToggle(command, false);
                        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.DJ_TOGGLED,
                                Pair.of("{command}", command.getName()),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        );
                    }
                    case "false" -> {
                        config.setDJToggle(command, true);
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
        final TogglesConfig config = TogglesConfig.getConfig(guild);

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

                switch (Boolean.toString(config.getLogToggle(logType))) {
                    case "true" -> {
                        config.setLogToggle(logType, false);
                        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.LOG_TOGGLED,
                                Pair.of("{logType}", logType.getName()),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        );
                    }
                    case "false" -> {
                        config.setLogToggle(logType, true);
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
            toggleStatuses.append(config.getDJToggle(toggle) ? RobertifyEmoji.CHECK_EMOJI.toString() : RobertifyEmoji.QUIT_EMOJI.toString())
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
            toggleStatuses.append(config.getLogToggle(toggle) ? RobertifyEmoji.CHECK_EMOJI.toString() : RobertifyEmoji.QUIT_EMOJI.toString())
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
        return "Aliases: `"+ GeneralUtils.listToString(getAliases())+"`\n" +
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
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;
        final var guild = event.getGuild();
        assert guild != null;

        event.deferReply().queue();

        final var config = TogglesConfig.getConfig(guild);
        final var path = event.getFullCommandName().split("\\s");
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
                    toggleStatuses.append(config.getToggle(toggle) ? RobertifyEmoji.CHECK_EMOJI.toString() : RobertifyEmoji.QUIT_EMOJI.toString())
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
                        if (config.getToggle(Toggles.RESTRICTED_VOICE_CHANNELS)) {
                            config.setToggle(Toggles.RESTRICTED_VOICE_CHANNELS, false);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "`Restricted Voice Channels`"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                            );
                        } else {
                            config.setToggle(Toggles.RESTRICTED_VOICE_CHANNELS, true);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "`Restricted Voice Channels`"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                            );
                        }
                    }
                    case "restrictedtext", "2", "rtc", "rtchannels" -> {
                        if (config.getToggle(Toggles.RESTRICTED_TEXT_CHANNELS)) {
                            config.setToggle(Toggles.RESTRICTED_TEXT_CHANNELS, false);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "`Restricted Text Channels`"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                            );
                        } else {
                            config.setToggle(Toggles.RESTRICTED_TEXT_CHANNELS, true);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "`Restricted Text Channels`"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                            );
                        }
                    }
                    case "announcements", "3" -> {
                        if (config.getToggle(Toggles.ANNOUNCE_MESSAGES)) {
                            config.setToggle(Toggles.ANNOUNCE_MESSAGES, false);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "`Announcing Player Messages`"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                            );
                        } else {
                            config.setToggle(Toggles.ANNOUNCE_MESSAGES, true);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "`Announcing Player Messages`"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                            );
                        }
                    }
                    case "requester", "4" -> {
                        if (config.getToggle(Toggles.SHOW_REQUESTER)) {
                            config.setToggle(Toggles.SHOW_REQUESTER, false);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "showing the requester in now playing messages"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                            );
                        } else {
                            config.setToggle(Toggles.SHOW_REQUESTER, true);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "showing the requester in now playing messages"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                            );
                        }
                    }
                    case "8ball", "5" -> {
                        if (config.getToggle(Toggles.EIGHT_BALL)) {
                            config.setToggle(Toggles.EIGHT_BALL, false);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "8ball command"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                            );
                        } else {
                            config.setToggle(Toggles.EIGHT_BALL, true);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "8ball command"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                            );
                        }
                    }
                    case "polls", "poll", "6" -> {
                        if (config.getToggle(Toggles.POLLS)) {
                            config.setToggle(Toggles.POLLS, false);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "polls command"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                            );
                        } else {
                            config.setToggle(Toggles.POLLS, true);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "polls command"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                            );
                        }
                    }
                    case "reminders", "7" -> {
                        if (config.getToggle(Toggles.REMINDERS)) {
                            config.setToggle(Toggles.REMINDERS, false);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "reminders"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                            );
                        } else {
                            config.setToggle(Toggles.REMINDERS, true);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "reminders"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                            );
                        }
                    }
                    case "tips", "8" -> {
                        if (config.getToggle(Toggles.TIPS)) {
                            config.setToggle(Toggles.TIPS, false);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "tips"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                            );
                        } else {
                            config.setToggle(Toggles.TIPS, true);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "tips"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                            );
                        }
                    }
                    case "voteskips", "voteskip", "vs", "9" -> {
                        if (config.getToggle(Toggles.VOTE_SKIPS)) {
                            config.setToggle(Toggles.VOTE_SKIPS, false);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "vote skips"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                            );

                            final var skipCommand = SlashCommandManager.getInstance().getCommand("skip");
                            if (config.getDJToggle(skipCommand)) {
                                event.getChannel().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.SKIP_DJ_TOGGLE_PROMPT).build())
                                        .setActionRow(
                                                Button.of(ButtonStyle.SUCCESS, "toggledjskip:yes:" + event.getUser().getId(), "", RobertifyEmoji.CHECK_EMOJI.getEmoji()),
                                                Button.of(ButtonStyle.SECONDARY, "toggledjskip:no:" + event.getUser().getId(), "", RobertifyEmoji.QUIT_EMOJI.getEmoji())
                                        )
                                        .queue();
                            }
                        } else {
                            config.setToggle(Toggles.VOTE_SKIPS, true);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.TOGGLED,
                                    Pair.of("{toggle}", "vote skips"),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                            );

                            final var skipCommand = SlashCommandManager.getInstance().getCommand("skip");
                            if (!config.getDJToggle(skipCommand))
                                config.setDJToggle(skipCommand, true);
                        }
                    }
                    default -> eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.INVALID_TOGGLE);
                }
            }
            case "dj" -> {
                switch (path[2]) {
                    case "list" -> eb = getDJTogglesEmbed(guild, SlashCommandManager.getInstance(), config);
                    case "switch" -> {
                        final String toggle = event.getOption("toggle").getAsString();
                        AbstractSlashCommand command = SlashCommandManager.getInstance().getCommand(toggle);

                        if (command == null) {
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.DJ_TOGGLE_INVALID_COMMAND, Pair.of("{command}", toggle));
                            break;
                        }

                        if (config.getDJToggle(command)) {
                            config.setDJToggle(command, false);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.DJ_TOGGLED,
                                    Pair.of("{command}", command.getName()),
                                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                            );
                        } else {
                            config.setDJToggle(command, true);
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.DJ_TOGGLED,
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

                        switch (Boolean.toString(config.getLogToggle(logType))) {
                            case "true" -> {
                                config.setLogToggle(logType, false);
                                eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.LOG_TOGGLED,
                                        Pair.of("{logType}", logType.getName()),
                                        Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                                );
                            }
                            case "false" -> {
                                config.setLogToggle(logType, true);
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
        event.getHook().sendMessageEmbeds(eb.build())
                .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getGuildChannel()))
                .queue();
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.getButton().getId().startsWith("toggledjskip:"))
            return;

        final var guild = event.getGuild();
        final var split = event.getButton().getId().split(":");
        if (!event.getUser().getId().equals(split[2])) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NO_PERMS_BUTTON).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final var disabledButtons = event.getMessage().getButtons()
                .stream()
                .map(Button::asDisabled)
                .toList();

        switch (split[1].toLowerCase()) {
            case "yes" -> {
                final var skipCommand = SlashCommandManager.getInstance().getCommand("skip");
                final var config = TogglesConfig.getConfig(guild);
                final var localeManager = LocaleManager.getLocaleManager(guild);

                config.setDJToggle(skipCommand, false);
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TogglesMessages.DJ_TOGGLED,
                        Pair.of("{command}", "skip"),
                        Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS))
                ).build()).setEphemeral(true).queue();
            }
            case "no" -> event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.OK).build())
                    .setEphemeral(true)
                    .queue();
        }

        event.getMessage().editMessageComponents(
                ActionRow.of(disabledButtons)
        ).queue();
    }
}
