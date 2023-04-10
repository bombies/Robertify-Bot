package main.utils.component.interactions;

import lombok.Getter;
import lombok.SneakyThrows;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.RandomMessageManager;
import main.commands.contextcommands.ContextCommandManager;
import main.commands.slashcommands.SlashCommandManager;
import main.constants.BotConstants;
import main.constants.Permission;
import main.constants.Toggles;
import main.main.Config;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.AbstractInteraction;
import main.utils.component.InvalidBuilderException;
import main.utils.database.mongodb.cache.BotBDCache;
import main.utils.json.requestchannel.RequestChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.restrictedchannels.RestrictedChannelsConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.*;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public abstract class AbstractSlashCommand extends AbstractInteraction {
    private final static Logger logger = LoggerFactory.getLogger(AbstractSlashCommand.class);

    private Command command = null;

    public String getName() {
        if (command == null)
            buildCommand();
        return command.name;
    }

    public String getDescription() {
        if (command == null)
            buildCommand();
        return command.description;
    }

    public boolean isPremium(){
        if (command == null)
            buildCommand();
        return command.isPremium;
    }

    public boolean isDevCommand() {
        if (command == null)
            buildCommand();
        return command.isPrivate;
    }
    
    public boolean isGuildCommand() {
        if (command == null)
            buildCommand();
        return command.isGuild;
    }

    public List<SubCommand> getSubCommands() {
        if (command == null)
            buildCommand();
        return command.subCommands;
    }

    public List<SubCommandGroup> getSubCommandGroups() {
        if (command == null)
            buildCommand();
        return command.subCommandGroups;
    }

    public List<CommandOption> getOptions() {
        if (command == null)
            buildCommand();
        return command.getOptions();
    }

    public List<Permission> getUserRequiredPermissions() {
        if (command == null)
            buildCommand();
        return command.requiredPermissions;
    }

    public List<net.dv8tion.jda.api.Permission> getBotRequiredPermissions() {
        if (command == null)
            buildCommand();
        return command.botRequiredPermissions;
    }

    public CommandData getCommandData() {
        if (command == null)
            buildCommand();

        SlashCommandData commandData = Commands.slash(
                command.name, command.description
        );

        // Adding subcommands
        if (!command.getSubCommands().isEmpty() || !command.getSubCommandGroups().isEmpty()) {
            if (!command.getSubCommands().isEmpty()) {
                for (SubCommand subCommand : command.getSubCommands()) {
                    var subCommandData = new SubcommandData(subCommand.getName(), subCommand.getDescription());

                    // Adding options for subcommands
                    for (CommandOption options : subCommand.getOptions()) {
                        OptionData optionData = new OptionData(options.getType(), options.getName(), options.getDescription(), options.isRequired());
                        if (options.getChoices() != null)
                            for (String choices : options.getChoices())
                                optionData.addChoice(choices, choices);

                        subCommandData.addOptions(optionData);
                    }
                    commandData.addSubcommands(subCommandData);
                }
            }

            if (!command.getSubCommandGroups().isEmpty())
                for (var subCommandGroup : command.getSubCommandGroups())
                    commandData.addSubcommandGroups(subCommandGroup.build());
        } else {
            // Adding options for the main command
            for (CommandOption options : command.getOptions()) {
                OptionData optionData = new OptionData(options.getType(), options.getName(), options.getDescription(), options.isRequired());

                if (options.getChoices() != null)
                    for (String choices : options.getChoices())
                        optionData.addChoice(choices, choices);
                commandData.addOptions(optionData);
            }
        }

        commandData.setGuildOnly(command.isGuildUseOnly());
        return commandData;
    }


    @SneakyThrows
    public void loadCommand(Guild g) {
        buildCommand();

        if (command == null)
            throw new IllegalStateException("The command is null! Cannot load into guild.");
        
        if (!command.isGuild && !command.isPrivate)
            return;
        
        if (command.isPrivate && g.getOwnerIdLong() != Config.getOwnerID())
            return;

        logger.debug("Loading command \"{}\" into guild {}", command.name, g.getName());

        // Initial request builder
        CommandCreateAction commandCreateAction = g.upsertCommand(command.getName(), command.getDescription());

        // Adding subcommands
        if (!command.getSubCommands().isEmpty() || !command.getSubCommandGroups().isEmpty()) {
            if (!command.getSubCommands().isEmpty()) {
                for (SubCommand subCommand : command.getSubCommands()) {
                    var subCommandData = new SubcommandData(subCommand.getName(), subCommand.getDescription());

                    // Adding options for subcommands
                    for (CommandOption options : subCommand.getOptions()) {
                        OptionData optionData = new OptionData(options.getType(), options.getName(), options.getDescription(), options.isRequired());
                        if (options.getChoices() != null)
                            for (String choices : options.getChoices())
                                optionData.addChoice(choices, choices);

                        subCommandData.addOptions(optionData);
                    }
                    commandCreateAction = commandCreateAction.addSubcommands(subCommandData);
                }
            }

            if (!command.getSubCommandGroups().isEmpty())
                for (var subCommandGroup : command.getSubCommandGroups())
                    commandCreateAction = commandCreateAction.addSubcommandGroups(subCommandGroup.build());
        } else {
            // Adding options for the main command
            for (CommandOption options : command.getOptions()) {
                OptionData optionData = new OptionData(options.getType(), options.getName(), options.getDescription(), options.isRequired());

                if (options.getChoices() != null)
                    for (String choices : options.getChoices())
                        optionData.addChoice(choices, choices);
                commandCreateAction = commandCreateAction.addOptions(optionData);
            }
        }

        commandCreateAction.queueAfter(1, TimeUnit.SECONDS, null, new ErrorHandler()
                .handle(ErrorResponse.MISSING_ACCESS, e -> logger.warn("I couldn't create guild commands in {}", g.getName())));
    }

    public void unload(Guild g) {
        g.retrieveCommands().queue(commands -> {
            final net.dv8tion.jda.api.interactions.commands.Command matchedCommand = commands.stream()
                    .filter(command -> command.getName().equals(this.getName()))
                    .findFirst()
                    .orElse(null);

            if (matchedCommand == null) return;

            g.deleteCommandById(matchedCommand.getIdLong()).queue();
        });
    }

    public static void unloadAllCommands(Guild g) {
        if (g.getOwnerIdLong() != Config.getOwnerID()) {
            g.updateCommands().addCommands().queue();
        } else {
            g.updateCommands()
                    .addCommands(SlashCommandManager.getInstance()
                            .getDevCommands()
                            .stream()
                            .map(AbstractSlashCommand::getCommandData)
                            .toList()
                    )
                    .queue(null, new ErrorHandler().handle(ErrorResponse.MISSING_ACCESS, e -> {
                        logger.error("I didn't have enough permission to unload guilds commands from {}", g.getName());
                    }));
        }
    }

    @SneakyThrows
    public void loadCommand() {
        buildCommand();
        
        if (command == null)
            throw new IllegalStateException("The command is null! Cannot load globally.");

        if (command.isGuild)
            return;

        if (command.isPrivate)
            return;

        // Initial request builder
        for (final var jda : Robertify.getShardManager().getShards()) {
            CommandCreateAction commandCreateAction = jda.upsertCommand(command.getName(), command.getDescription());

            // Adding subcommands
            if (!command.getSubCommands().isEmpty() || !command.getSubCommandGroups().isEmpty()) {
                if (!command.getSubCommands().isEmpty()) {
                    for (SubCommand subCommand : command.getSubCommands()) {
                        var subCommandData = new SubcommandData(subCommand.getName(), subCommand.getDescription());

                        // Adding options for subcommands
                        for (CommandOption options : subCommand.getOptions()) {
                            OptionData optionData = new OptionData(options.getType(), options.getName(), options.getDescription(), options.isRequired());
                            if (options.getChoices() != null)
                                for (String choices : options.getChoices())
                                    optionData.addChoice(choices, choices);

                            subCommandData.addOptions(optionData);
                        }
                        commandCreateAction = commandCreateAction.addSubcommands(subCommandData);
                    }
                }

                if (!command.getSubCommandGroups().isEmpty())
                    for (var subCommandGroup : command.getSubCommandGroups())
                        commandCreateAction = commandCreateAction.addSubcommandGroups(subCommandGroup.build());
            } else {
                // Adding options for the main command
                for (CommandOption options : command.getOptions()) {
                    OptionData optionData = new OptionData(options.getType(), options.getName(), options.getDescription(), options.isRequired());

                    if (options.getChoices() != null)
                        for (String choices : options.getChoices())
                            optionData.addChoice(choices, choices);
                    commandCreateAction = commandCreateAction.addOptions(optionData);
                }
            }

            commandCreateAction = commandCreateAction.setGuildOnly(command.guildUseOnly)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(
                            command.botRequiredPermissions
                    ));

            commandCreateAction.queueAfter(1, TimeUnit.SECONDS, cmd -> {

            }, new ErrorHandler()
                    .handle(ErrorResponse.MISSING_ACCESS, e -> {}));
        }
    }

    public void unload() {
        for (final var jda : Robertify.getShardManager().getShards()) {
            jda.retrieveCommands().queue(commands -> {
                final net.dv8tion.jda.api.interactions.commands.Command matchedCommand = commands.stream()
                        .filter(command -> command.getName().equals(this.getName()))
                        .findFirst()
                        .orElse(null);

                if (matchedCommand == null) return;

                jda.deleteCommandById(matchedCommand.getIdLong()).queue();
            });
        }
    }

    public void reload() {
        if (this.isGuildCommand())
            return;
        upsertCommand(this);
    }

    public static void loadAllCommands(Guild g) {
        SlashCommandManager slashCommandManager = SlashCommandManager.getInstance();
        List<AbstractSlashCommand> commands = slashCommandManager.getGuildCommands();
        List<AbstractSlashCommand> devCommands = slashCommandManager.getDevCommands();
        CommandListUpdateAction commandListUpdateAction = g.updateCommands();

        ContextCommandManager contextCommandManager = new ContextCommandManager();
        List<AbstractContextCommand> contextCommands = contextCommandManager.getCommands();

        for (var cmd : commands)
            commandListUpdateAction = commandListUpdateAction.addCommands(
                    cmd.getCommandData()
            );

        for (var cmd : contextCommands)
            commandListUpdateAction = commandListUpdateAction.addCommands(
                    cmd.getCommandData()
            );

        if (g.getOwnerIdLong() == Config.getOwnerID()) {
            for (var cmd : devCommands)
                commandListUpdateAction = commandListUpdateAction.addCommands(
                        cmd.getCommandData()
                );
        }

        commandListUpdateAction.queueAfter(1, TimeUnit.SECONDS, null, new ErrorHandler()
                .handle(ErrorResponse.fromCode(30034), e -> g.retrieveOwner().queue(
                    owner -> owner.getUser()
                            .openPrivateChannel().queue(channel -> {
                                channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(g, "Hey, I could not create slash commands in **"+g.getName()+"**" +
                                                " due to being re-invited too many times. Try inviting me again tomorrow to fix this issue.").build())
                                        .queue(null, new ErrorHandler()
                                                .handle(ErrorResponse.CANNOT_SEND_TO_USER, ex2 -> {}));
                            })
                    )
                )
                .handle(ErrorResponse.MISSING_ACCESS, e -> {
                    logger.warn("I wasn't able to update commands in {}!", g.getName());
//                    g.retrieveOwner().queue(owner ->
//                        owner.getUser().openPrivateChannel().queue(channel ->
//                            channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(g, "Hey, it seems I don't have permission to make slash commands in **"+ g.getName() +"**. 🙁\n" +
//                                                    "It's essential you enable this permission for me because this is the main entry point for Robertify. All the latest features will only be accessible through slash commands." +
//                                                    "\n\nYou can click on the button below to re-invite me with the correct permissions. 🙂")
//                                    .build())
//                                    .setActionRow(Button.link("https://discord.com/oauth2/authorize?client_id=893558050504466482&permissions=269479308656&scope=bot%20applications.commands", "Invite Me!"))
//                                    .queue(null, new ErrorHandler()
//                                            .handle(ErrorResponse.CANNOT_SEND_TO_USER, e2 -> logger.info("I wasn't able to send a private message to the owner of the guild {}", g.getName())))
//                        )
//                    );
                })
        );
    }

    public static void loadAllCommands() {
        SlashCommandManager slashCommandManager = SlashCommandManager.getInstance();
        List<AbstractSlashCommand> commands = slashCommandManager.getGlobalCommands();

        for (final var jda : Robertify.getShardManager().getShards()) {
            CommandListUpdateAction commandListUpdateAction = jda.updateCommands();

            ContextCommandManager contextCommandManager = new ContextCommandManager();
            List<AbstractContextCommand> contextCommands = contextCommandManager.getCommands();

            for (var cmd : commands)
                commandListUpdateAction = commandListUpdateAction.addCommands(
                        cmd.getCommandData()
                );

            for (var cmd : contextCommands)
                commandListUpdateAction = commandListUpdateAction.addCommands(
                        cmd.getCommandData()
                );

            commandListUpdateAction.queueAfter(1, TimeUnit.SECONDS);
        }
    }

    public static void upsertCommand(AbstractSlashCommand command) {
        for (final var jda : Robertify.getShardManager().getShards()) {
            jda.upsertCommand(command.getCommandData()).queue();
        }
    }



    protected void setCommand(Command command) {
        this.command = command;
    }

    protected Command getCommand() {
        if (command == null)
            buildCommand();
        return command;
    }

    protected void sendRandomMessage(SlashCommandInteractionEvent event) {
        if (command == null)
            buildCommand();
        if (SlashCommandManager.getInstance().isMusicCommand(this) && event.getChannel().getType().isMessage())
            new RandomMessageManager().randomlySendMessage(event.getChannel().asGuildMessageChannel());
    }

    protected boolean checks(SlashCommandInteractionEvent event) {
        if (!nameCheck(event)) return false;
        if (!guildCheck(event)) return false;
        if (!botEmbedCheck(event)) return false;
        if (!banCheck(event)) return false;
        if (!restrictedChannelCheck(event)) return false;
        if (!botPermsCheck(event)) return false;
        if (!premiumBotCheck(event)) return false;

        if (command == null) buildCommand();
        if (SlashCommandManager.getInstance().isMusicCommand(this)) {
            final var botDB = BotBDCache.getInstance();
            final var latestAlert = botDB.getLatestAlert().getLeft();
            final var user = event.getUser();

            if (
                    !botDB.userHasViewedAlert(user.getIdLong())
                    && (!latestAlert.isEmpty() && !latestAlert.isBlank())
                    && SlashCommandManager.getInstance().isMusicCommand(this)
            )
                event.getChannel().asGuildMessageChannel().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), RobertifyLocaleMessage.GeneralMessages.UNREAD_ALERT_MENTION, Pair.of("{user}", user.getAsMention())).build())
                        .queue(msg -> {
                            final var dedicatedChannelConfig = new RequestChannelConfig(msg.getGuild());
                            if (dedicatedChannelConfig.isChannelSet())
                                if (dedicatedChannelConfig.getChannelID() == msg.getChannel().getIdLong())
                                    msg.delete().queueAfter(10, TimeUnit.SECONDS, null, new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, e -> {}));
                        }, new ErrorHandler().handle(ErrorResponse.MISSING_PERMISSIONS, e -> {
                            if (!e.getMessage().contains("MESSAGE_SEND"))
                                logger.error("Unexpected error when attempting to send an unread alert message", e.getCause());
                        }));
        }

        if (!adminCheck(event)) return false;
        if (!djCheck(event)) return false;

        if (SlashCommandManager.getInstance().isMusicCommand(this) && event.isFromGuild()) {
            assert event.getGuild() != null;
            final var scheduler = RobertifyAudioManager.getInstance()
                    .getMusicManager(event.getGuild())
                    .getScheduler();
            scheduler.setAnnouncementChannel(event.getGuildChannel());
        }

        return true;
    }

    /**
     * Conducts all the normal checks but with a check to see if the guild the command is
     * coming from is a premium guild.
     * @param event
     * @return True if all the checks have been passed, false otherwise.
     */
    protected boolean checksWithPremium(SlashCommandInteractionEvent event) {
        return checks(event);
//        return premiumCheck(event);
    }

    /**
     * Checks if the name of the command in the event passed is the same name as this command object
     * @param event
     * @return True if the names are the same, false if otherwise.
     */
    protected boolean nameCheck(SlashCommandInteractionEvent event) {
        if (command == null)
            buildCommand();
        return command.getName().equals(event.getName());
    }

    protected boolean guildCheck(SlashCommandInteractionEvent event) {
        if (command == null)
            buildCommand();
        if (command.isGuild)
            return true;
        if (!event.isFromGuild() && command.guildUseOnly) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(RobertifyLocaleMessage.GeneralMessages.GUILD_COMMAND_ONLY).build())
                    .queue();
            return false;
        }
        return true;
    }

    /***
     * Checks if the user who is attempting to execute the command is a banned user
     * @param event
     * @return True if the user is banned, false if otherwise.
     */
    protected boolean banCheck(SlashCommandInteractionEvent event) {
        final Guild guild = event.getGuild();
        if (guild == null)
            return true;
        if (!new GuildConfig(guild).isBannedUser(event.getUser().getIdLong()))
            return true;

        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.BANNED_FROM_COMMANDS).build())
                .setEphemeral(true)
                .queue();
        return false;
    }

    /**
     * Checks if the command is a possible DJ command.
     * If the command is a DJ command it then checks if
     * the author of the command has permission to run the command
     * @param event The slash command event.
     * @return True - If the command isn't a DJ command or the user is a DJ
     *         False - If the command is a DJ command and the user isn't a DJ.
     */
    protected boolean musicCommandDJCheck(SlashCommandInteractionEvent event) {
        return predicateCheck(event);
    }

    /**
     * Checks if the command is a premium command.
     * If the command is a premium command it then
     * checks if the author of the command is a premium user.
     * @param event The slash command event.
     * @return True - If the command isn't a premium command or the user is a premium user
     *         False - If the command is a premium command and the user isn't a premium user
     */
    protected boolean premiumCheck(SlashCommandInteractionEvent event) {
        if (command == null)
            buildCommand();

        if (!command.isPremium)
            return true;

        User user = event.getUser();
        if (!GeneralUtils.checkPremium(event.getGuild(), event) && user.getIdLong() != 276778018440085505L)
            return false;

        return true;
    }

    protected boolean premiumBotCheck(SlashCommandInteractionEvent event) {
        if (command == null)
            buildCommand();
        if (!Config.isPremiumBot())
            return true;

        final var guild = event.getGuild();
        if (guild == null)
            return true;

        if (!new GuildConfig(guild).isPremium()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild, RobertifyLocaleMessage.GeneralMessages.PREMIUM_EMBED_TITLE, RobertifyLocaleMessage.GeneralMessages.PREMIUM_INSTANCE_NEEDED).build())
                    .addActionRow(Button.link("https://robertify.me/premium", LocaleManager.getLocaleManager(guild).getMessage(RobertifyLocaleMessage.GeneralMessages.PREMIUM_UPGRADE_BUTTON)))
                    .queue();
            return false;
        }

        return true;
    }

    /***
     * Checks if the user executing the command is a DJ or not if the command is DJ-only.
     * @param event
     * @return If the command is a DJ-only command, true will be returned if the user
     *         attempting to execute the command is a DJ. If the command is not DJ-only,
     *         true will be returned.
     *         False will be returned if and only if the command is DJ-only and the user is not a DJ.
     */
    protected boolean djCheck(SlashCommandInteractionEvent event) {
        if (command == null)
            buildCommand();

        final Guild guild = event.getGuild();
        if (
                command.isDjOnly()
                && !GeneralUtils.hasPerms(guild, event.getMember(), Permission.ROBERTIFY_DJ)
                && !GeneralUtils.isDeveloper(event.getUser().getIdLong())
        ) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, BotConstants.getInsufficientPermsMessage(guild, Permission.ROBERTIFY_DJ)).build())
                    .setEphemeral(true)
                    .queue();
            return false;
        }
        return true;
    }

    /***
     * Checks if the user executing the command is an admin or not if the command is admin-only.
     * @param event
     * @return If the command is an admin-only command, true will be returned if the user
     *         attempting to execute the command is an admin. If the command is not admin-only,
     *         true will be returned.
     *         False will be returned if and only if the command is admin-only and the user is not an admin.
     */
    protected boolean adminCheck(SlashCommandInteractionEvent event) {
        if (command == null)
            buildCommand();

        final Guild guild = event.getGuild();
        if (
                command.adminOnly
                && !GeneralUtils.hasPerms(guild, event.getMember(), Permission.ROBERTIFY_ADMIN)
                && !GeneralUtils.isDeveloper(event.getUser().getIdLong())
        ) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, BotConstants.getInsufficientPermsMessage(guild, Permission.ROBERTIFY_ADMIN)).build())
                    .setEphemeral(true)
                    .queue();
            return false;
        }
        return true;
    }

    protected boolean predicateCheck(SlashCommandInteractionEvent event) {
        if (command == null)
            buildCommand();
        if (command.getCheckPermission() == null)
            return true;
        if (GeneralUtils.isDeveloper(event.getId()))
            return true;
        return command.getCheckPermission().test(event);
    }

    protected boolean botPermsCheck(SlashCommandInteractionEvent event) {
        if (command == null)
            buildCommand();
        if (command.botRequiredPermissions.isEmpty())
            return true;

        final Guild guild = event.getGuild();
        if (guild == null)
            return true;

        final var self = guild.getSelfMember();
        if (!self.hasPermission(command.botRequiredPermissions)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SELF_INSUFFICIENT_PERMS_ARGS, Pair.of("{permissions}", GeneralUtils.listToString(command.botRequiredPermissions))).build())
                    .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()))
                    .queue();
            return false;
        }
        return true;
    }

    protected boolean botEmbedCheck(SlashCommandInteractionEvent event) {
        final var guild = event.getGuild();
        if (guild == null)
            return true;
        if (!guild.getSelfMember().hasPermission(event.getGuildChannel(), net.dv8tion.jda.api.Permission.MESSAGE_EMBED_LINKS)) {
            event.reply(LocaleManager.getLocaleManager(guild).getMessage(RobertifyLocaleMessage.GeneralMessages.NO_EMBED_PERMS))
                    .queue();
            return false;
        }
        return true;
    }

    protected boolean restrictedChannelCheck(SlashCommandInteractionEvent event) {
        final Guild guild = event.getGuild();
        if (guild == null)
            return true;

        final TogglesConfig togglesConfig = TogglesConfig.getConfig(guild);
        final RestrictedChannelsConfig config = new RestrictedChannelsConfig(guild);

        if (!togglesConfig.getToggle(Toggles.RESTRICTED_TEXT_CHANNELS))
            return true;

        if (
                !config.isRestrictedChannel(event.getChannel().getIdLong(), RestrictedChannelsConfig.ChannelType.TEXT_CHANNEL)
                        && !GeneralUtils.hasPerms(guild, event.getMember(), Permission.ROBERTIFY_ADMIN)
                        && !GeneralUtils.isDeveloper(event.getUser().getIdLong())
        ) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.CANT_BE_USED_IN_CHANNEL_ARGS,
                    Pair.of("{channels}", GeneralUtils.listOfIDsToMentions(
                            guild,
                            config.getRestrictedChannels(RestrictedChannelsConfig.ChannelType.TEXT_CHANNEL),
                            GeneralUtils.Mentioner.CHANNEL
                    ))).build())
                    .setEphemeral(true)
                    .queue();
            return false;
        }
        return true;
    }

    protected boolean devCheck(SlashCommandInteractionEvent event) {
        if (command == null)
            buildCommand();
        if (!nameCheck(event))
            return false;
        if (!command.isPrivate)
            return true;

        if (!BotBDCache.getInstance().isDeveloper(event.getUser().getIdLong())) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), RobertifyLocaleMessage.GeneralMessages.INSUFFICIENT_PERMS_NO_ARGS).build())
                    .setEphemeral(true)
                    .queue();
            return false;
        }
        return true;
    }

    protected abstract void buildCommand();
    public abstract String getHelp();

    public String getUsages() {
        return null;
    }

    protected Builder getBuilder() {
        return new Builder();
    }

    protected static class Command {
        @Getter @NotNull
        private final String name;
        @Getter @NotNull
        private final String description;
        @Getter @NotNull
        private final List<CommandOption> options;
        @Getter @NotNull
        private final List<SubCommandGroup> subCommandGroups;
        @Getter @NotNull
        private final List<SubCommand> subCommands;
        @Getter
        private final List<Permission> requiredPermissions;
        @Getter
        private final List<net.dv8tion.jda.api.Permission> botRequiredPermissions;
        @Nullable @Getter
        private final Predicate<SlashCommandInteractionEvent> checkPermission;
        @Getter
        private final boolean djOnly;
        @Getter
        private final boolean adminOnly;
        @Getter
        private final boolean isPremium;
        @Getter
        private final boolean isPrivate;
        @Getter
        private final boolean isGuild;
        @Getter
        private final boolean guildUseOnly;

        private Command(
                @NotNull String name,
                @Nullable String description,
                @NotNull List<CommandOption> options,
                @NotNull List<SubCommandGroup> subCommandGroups,
                @NotNull List<SubCommand> subCommands,
                @Nullable Predicate<SlashCommandInteractionEvent> checkPermission,
                boolean djOnly,
                boolean adminOnly,
                boolean isPremium,
                boolean isPrivate,
                List<Permission> requiredPermissions,
                List<net.dv8tion.jda.api.Permission> botRequiredPermissions,
                boolean isGuild,
                boolean guildUseOnly
        ) {
            this.name = name.toLowerCase();
            this.description = description == null ? "" : description;
            this.options = options;
            this.subCommandGroups = subCommandGroups;
            this.subCommands = subCommands;
            this.checkPermission = checkPermission;
            this.djOnly = djOnly;
            this.adminOnly = adminOnly;
            this.isPremium = isPremium;
            this.isPrivate = isPrivate;
            this.requiredPermissions = requiredPermissions;
            this.botRequiredPermissions = botRequiredPermissions;
            this.isGuild = isGuild;
            this.guildUseOnly = guildUseOnly;
        }

        public boolean permissionCheck(SlashCommandInteractionEvent e) {
            if (checkPermission == null)
                throw new NullPointerException("Can't perform permission check since a check predicate was not provided!");

            return checkPermission.test(e);
        }

        public static Command of(String name, String description, List<CommandOption> options, List<SubCommandGroup> subCommandGroups, List<SubCommand> subCommands, Predicate<SlashCommandInteractionEvent> checkPermission) {
            return new Command(name, description, options, subCommandGroups, subCommands, checkPermission, false, false, false, false, List.of(), List.of(), false, true);
        }

        public static Command of(String name, String description, List<CommandOption> options, List<SubCommand> subCommands, Predicate<SlashCommandInteractionEvent> checkPermission) {
            return new Command(name, description, options, List.of(), subCommands, checkPermission, false, false, false, false, List.of(), List.of(), false, true);
        }

        public static Command of(String name, String description, List<CommandOption> options,
                                                    List<SubCommand> subCommands, Predicate<SlashCommandInteractionEvent> checkPermission, boolean djOnly) {
            return new Command(name, description, options, List.of(), subCommands, checkPermission, djOnly, false, false, false, List.of(), List.of(), false, true);
        }

        public static Command of(String name, String description, List<CommandOption> options,
                                                    List<SubCommand> subCommands, Predicate<SlashCommandInteractionEvent> checkPermission, boolean djOnly, boolean adminOnly) {
            return new Command(name, description, options, List.of(), subCommands, checkPermission, false, adminOnly, false, false, List.of(), List.of(), false, true);
        }

        public static Command of(String name, String description, List<CommandOption> options, List<SubCommand> subCommands) {
            return new Command(name, description, options, List.of(), subCommands, null, false, false, false, false, List.of(), List.of(), false, true);
        }

        public static Command of(String name, String description, List<CommandOption> options,
                                                    List<SubCommand> subCommands, boolean djOnly) {
            return new Command(name, description, options, List.of(), subCommands, null, djOnly, false, false, false, List.of(), List.of(), false, true);
        }

        public static Command of(String name, String description, List<CommandOption> options,
                                                    List<SubCommand> subCommands, boolean djOnly, boolean adminOnly) {
            return new Command(name, description, options, List.of(), subCommands, null, false, adminOnly, false, false, List.of(), List.of(), false, true);
        }

        public static Command of(String name, String description, List<CommandOption> options, Predicate<SlashCommandInteractionEvent> checkPermission) {
            return new Command(name, description, options, List.of(), List.of(), checkPermission, false, false, false, false, List.of(), List.of(), false, true);
        }

        public static Command of(String name, String description, List<CommandOption> options,
                                                    Predicate<SlashCommandInteractionEvent> checkPermission, boolean djOnly) {
            return new Command(name, description, options, List.of(), List.of(), checkPermission, djOnly, false, false, false, List.of(), List.of(), false, true);
        }

        public static Command of(String name, String description, List<CommandOption> options,
                                                    Predicate<SlashCommandInteractionEvent> checkPermission, boolean djOnly, boolean adminOnly) {
            return new Command(name, description, options, List.of(), List.of(), checkPermission, false, adminOnly, false, false, List.of(), List.of(), false, true);
        }

        public static Command of(String name, String description, List<CommandOption> options) {
            return new Command(name, description, options, List.of(), List.of(), null, false, false, false, false, List.of(), List.of(), false, true);
        }

        public static Command of(String name, String description, List<CommandOption> options, boolean djOnly) {
            return new Command(name, description, options, List.of(), List.of(), null, djOnly, false, false, false, List.of(), List.of(), false, true);
        }

        public static Command of(String name, String description, List<CommandOption> options,
                                                    boolean djOnly, boolean adminOnly) {
            return new Command(name, description, options, List.of(), List.of(), null, false, adminOnly, false, false, List.of(), List.of(), false, true);
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands, Predicate<SlashCommandInteractionEvent> checkPermission) {
            return new Command(name, description, List.of(), List.of(), subCommands, checkPermission, false, false, false, false, List.of(), List.of(), false, true);
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands,
                                                           Predicate<SlashCommandInteractionEvent> checkPermission, boolean djOnly) {
            return new Command(name, description, List.of(), List.of(), subCommands, checkPermission, djOnly, false, false, false, List.of(), List.of(), false, true);
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands,
                                                           Predicate<SlashCommandInteractionEvent> checkPermission, boolean djOnly, boolean adminOnly) {
            return new Command(name, description, List.of(), List.of(), subCommands, checkPermission, djOnly, adminOnly, false, false, List.of(), List.of(), false, true);
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands) {
            return new Command(name, description, List.of(), List.of(), subCommands, null, false, false, false, false, List.of(), List.of(), false, true);
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands, List<SubCommandGroup> subCommandGroups) {
            return new Command(name, description, List.of(), subCommandGroups, subCommands, null, false, false, false, false, List.of(), List.of(), false, true);
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands,
                                                           boolean djOnly) {
            return new Command(name, description, List.of(), List.of(), subCommands, null, djOnly, false, false, false, List.of(), List.of(), false, true);
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands,
                                                           boolean djOnly, boolean adminOnly) {
            return new Command(name, description, List.of(), List.of(), subCommands, null, false, adminOnly, false, false, List.of(), List.of(), false, true);
        }

        public static Command of(String name, String description, Predicate<SlashCommandInteractionEvent> checkPermission) {
            return new Command(name, description, List.of(), List.of(), List.of(), checkPermission, false, false, false, false, List.of(), List.of(), false, true);
        }

        public static Command of(String name, String description,
                                                    Predicate<SlashCommandInteractionEvent> checkPermission, boolean djOnly) {
            return new Command(name, description, List.of(), List.of(), List.of(), checkPermission, djOnly, false, false, false, List.of(), List.of(), false, true);
        }

        public static Command of(String name, String description,
                                                    Predicate<SlashCommandInteractionEvent> checkPermission, boolean djOnly, boolean adminOnly) {
            return new Command(name, description, List.of(), List.of(), List.of(), checkPermission, false, adminOnly, false, false, List.of(), List.of(), false, true);
        }

        public static Command of(String name, String description) {
            return new Command(name, description, List.of(), List.of(), List.of(), null, false, false, false, false, List.of(), List.of(), false, true);
        }

        public static Command of(String name, String description, boolean djOnly) {
            return new Command(name, description, List.of(), List.of(), List.of(), null, djOnly, false, false, false, List.of(), List.of(), false, true);
        }

        public static Command of(String name, String description, boolean djOnly, boolean adminOnly) {
            return new Command(name, description, List.of(), List.of(), List.of(), null, false, adminOnly, false, false, List.of(), List.of(), false, true);
        }
    }

    @lombok.Builder
    protected static class SubCommand {
        @NotNull @Getter
        private final String name;
        @Nullable @Getter
        private final String description;
        @Getter
        private final List<CommandOption> options;


        public SubCommand(@NotNull String name, @Nullable String description, @NotNull List<CommandOption> options) {
            this.name = name.toLowerCase();
            this.description = description;
            this.options = options;
        }

        public static SubCommand of(String name, String description, List<CommandOption> options) {
            return new SubCommand(name, description, options);
        }

        public static SubCommand of(String name, String description) {
            return new SubCommand(name, description, List.of());
        }
    }

    protected static class SubCommandGroup {
        @NotNull
        private final String name;
        private final String description;
        private final List<SubCommand> subCommands;

        private SubCommandGroup(@NotNull String name, String description, List<SubCommand> subCommands) {
            this.name = name;
            this.description = description;
            this.subCommands = subCommands;
        }

        public static SubCommandGroup of(String name, String description, List<SubCommand> subCommands) {
            return new SubCommandGroup(name, description, subCommands);
        }

        @SneakyThrows
        public SubcommandGroupData build() {

            SubcommandGroupData data = new SubcommandGroupData(name, description);

            for (var command : subCommands) {
                SubcommandData subcommandData = new SubcommandData(command.name, command.description);

                for (var option : command.getOptions()) {
                    OptionData optionData = new OptionData(
                            option.getType(),
                            option.getName(),
                            option.getDescription(),
                            option.isRequired()
                    );

                    if (option.getChoices() != null)
                        for (final var choice : option.getChoices())
                            optionData.addChoice(choice, choice);
                    subcommandData.addOptions(optionData);
                };

                data.addSubcommands(subcommandData);
            }

            return data;
        }
    }

    @lombok.Builder
    protected static class CommandOption {
        @Getter
        private final OptionType type;
        @Getter
        private final String name;
        @Getter
        private final String description;
        @Getter
        private final boolean required;
        @Getter
        private final List<String> choices;

        private CommandOption(OptionType type, String name, String description, boolean required, List<String> choices) {
            this.type = type;
            this.name = name.toLowerCase();
            this.description = description;
            this.required = required;
            this.choices = choices;
        }

        public static CommandOption of(OptionType type, String name, String description, boolean required) {
            return new CommandOption(type, name, description, required, null);
        }

        public static CommandOption of(OptionType type, String name, String description, boolean required, List<String> choices) {
            return new CommandOption(type, name, description, required, choices);
        }
    }

    protected static class Builder {
        private String name, description;
        private final List<CommandOption> options;
        private final List<SubCommand> subCommands;
        private final List<SubCommandGroup> subCommandGroups;
        private final List<Permission> requiredPermissions;
        private final List<net.dv8tion.jda.api.Permission> botRequiredPermissions;
        private Predicate<SlashCommandInteractionEvent> permissionCheck;
        private boolean djOnly, adminOnly, isPremium, isPrivate, isGuild, guildUseOnly;

        private Builder() {
            this.options = new ArrayList<>();
            this.subCommands = new ArrayList<>();
            this.subCommandGroups = new ArrayList<>();
            this.requiredPermissions = new ArrayList<>();
            this.botRequiredPermissions = new ArrayList<>();
            this.djOnly = false;
            this.adminOnly = false;
            this.isPremium = false;
            this.isPrivate = false;
            this.isGuild = false;
            this.guildUseOnly = true;
        }

        public Builder setName(@NotNull String name) {
            this.name = name.toLowerCase();
            return this;
        }

        public Builder setDescription(@NotNull String description) {
            this.description = description;
            return this;
        }

        public Builder addOptions(CommandOption... options) {
            this.options.addAll(Arrays.asList(options));
            return this;
        }

        public Builder addSubCommands(SubCommand... subCommands) {
            this.subCommands.addAll(Arrays.asList(subCommands));
            return this;
        }

        public Builder addSubCommandGroups(SubCommandGroup... subCommandGroups) {
            this.subCommandGroups.addAll(Arrays.asList(subCommandGroups));
            return this;
        }

        public Builder setPermissionCheck(Predicate<SlashCommandInteractionEvent> predicate) {
            this.permissionCheck = predicate;
            return this;
        }

        public Builder checkForPermissions(Permission... permissions) {
            this.permissionCheck = e -> GeneralUtils.hasPerms(e.getGuild(), e.getMember(), permissions);
            requiredPermissions.addAll(Arrays.asList(permissions));
            return this;
        }

        public Builder setBotRequiredPermissions(net.dv8tion.jda.api.Permission... permissions) {
            botRequiredPermissions.addAll(Arrays.asList(permissions));
            return this;
        }

        @SneakyThrows
        public Builder setPossibleDJCommand() {
            this.permissionCheck = e -> {
                final TogglesConfig config = TogglesConfig.getConfig(e.getGuild());

                if (!config.isDJToggleSet(e.getName()))
                    return true;

                if (config.getDJToggle(SlashCommandManager.getInstance().getCommand(e.getName())))
                    return GeneralUtils.hasPerms(e.getGuild(), e.getMember(), Permission.ROBERTIFY_DJ);

                return true;

            };
            return this;
        }

        @SneakyThrows
        public Builder setDJOnly() {
            if (adminOnly)
                throw new InvalidBuilderException("DJ-only mode for this command can't be set to true when it's already admin-only");
            djOnly = true;
            requiredPermissions.add(Permission.ROBERTIFY_DJ);
            return this;
        }

        public Builder setAdminOnly() {
            if (djOnly) {
                djOnly = false;
                requiredPermissions.remove(Permission.ROBERTIFY_DJ);
            }

            adminOnly = true;
            requiredPermissions.add(Permission.ROBERTIFY_ADMIN);
            return this;
        }

        public Builder setPremium() {
            this.isPremium = true;
            return this;
        }
        
        public Builder setGuildCommand() {
            this.isGuild = true;
            return this;
        }

        public Builder allowGlobalUse() {
            this.guildUseOnly = false;
            return this;
        }

        public Builder setDevCommand() {
            this.isPrivate = true;
            this.permissionCheck = e -> BotBDCache.getInstance().isDeveloper(e.getUser().getIdLong());
            return this;
        }

        @SneakyThrows
        public Command build() {
            if (name == null)
                throw new InvalidBuilderException("The name of the command can't be null!");
            if (name.isBlank())
                throw new InvalidBuilderException("The name of the command can't be empty!");
            if (description == null)
                throw new InvalidBuilderException("The description of the command can't be null!");
            if (description.isBlank())
                throw new InvalidBuilderException("The description of the command can't be empty!");
            if (!options.isEmpty() && (!subCommands.isEmpty() || !subCommandGroups.isEmpty()))
                throw new InvalidBuilderException("You can't provide command options with subcommands and/or subcommand groups!");

            return new Command(
                    name,
                    description,
                    options,
                    subCommandGroups,
                    subCommands,
                    permissionCheck,
                    djOnly,
                    adminOnly,
                    isPremium,
                    isPrivate,
                    requiredPermissions,
                    botRequiredPermissions,
                    isGuild,
                    guildUseOnly
            );
        }
    }
}
