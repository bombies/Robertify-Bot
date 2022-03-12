package main.utils.component.interactions;

import lombok.Getter;
import lombok.SneakyThrows;
import main.commands.RandomMessageManager;
import main.commands.slashcommands.SlashCommandManager;
import main.constants.BotConstants;
import main.constants.Permission;
import main.constants.Toggles;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.AbstractInteraction;
import main.utils.component.InvalidBuilderException;
import main.utils.database.mongodb.cache.BotInfoCache;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.restrictedchannels.RestrictedChannelsConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.votes.VoteManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import org.discordbots.api.client.DiscordBotListAPI;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public abstract class AbstractSlashCommand extends AbstractInteraction {
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


    @SneakyThrows
    public void loadCommand(Guild g) {
        buildCommand();

        if (command == null)
            throw new IllegalStateException("The command is null! Cannot load into guild.");

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

        if (command.isPrivate)
            commandCreateAction = commandCreateAction.setDefaultEnabled(false);

        commandCreateAction.queueAfter(1, TimeUnit.SECONDS, createdCommand -> {
            if (!command.isPrivate) return;

            List<Long> developers = BotInfoCache.getInstance().getDevelopers();
            developers.forEach(developer -> createdCommand.updatePrivileges(g, CommandPrivilege.enableUser(developer)).queue());
        }, new ErrorHandler()
                .handle(ErrorResponse.MISSING_ACCESS, e -> {}));
    }

    protected void setCommand(Command command) {
        this.command = command;
    }

    protected Command getCommand() {
        if (command == null)
            buildCommand();
        return command;
    }

    protected void sendRandomMessage(SlashCommandEvent event) {
        if (command == null)
            buildCommand();
        if (new SlashCommandManager().isMusicCommand(this))
            new RandomMessageManager().randomlySendMessage(event.getTextChannel());
    }

    protected boolean checks(SlashCommandEvent event) {
        if (!nameCheck(event)) return false;
        if (!botEmbedCheck(event)) return false;
        if (!banCheck(event)) return false;
        if (!restrictedChannelCheck(event)) return false;
        if (!botPermsCheck(event)) return false;
        if (!adminCheck(event)) return false;
        return djCheck(event);
    }

    protected boolean checksWithPremium(SlashCommandEvent event) {
        if (!checks(event)) return false;
        return premiumCheck(event);
    }

    protected boolean nameCheck(SlashCommandEvent event) {
        if (command == null)
            buildCommand();
        return command.getName().equals(event.getName());
    }

    protected boolean banCheck(SlashCommandEvent event) {
        final Guild guild = event.getGuild();
        if (!new GuildConfig().isBannedUser(guild.getIdLong(), event.getUser().getIdLong()))
            return true;

        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have been banned from using commands!").build())
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
    protected boolean musicCommandDJCheck(SlashCommandEvent event) {
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
    protected boolean premiumCheck(SlashCommandEvent event) {
        if (command == null)
            buildCommand();

        if (!command.isPremium)
            return true;

        DiscordBotListAPI topGGAPI = Robertify.getTopGGAPI();
        if (topGGAPI == null)
            return true;

        User user = event.getUser();
        if (!new VoteManager().userVoted(user.getId(), VoteManager.Website.TOP_GG)
                && user.getIdLong() != 276778018440085505L) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(event.getGuild(),
                            "🔒 Locked Command", """
                                                    Woah there! You must vote before interacting with this command.
                                                    Click on each of the buttons below to vote!""").build())
                    .addActionRow(
                            Button.of(ButtonStyle.LINK, "https://top.gg/bot/893558050504466482/vote", "Top.gg"),
                            Button.of(ButtonStyle.LINK, "https://discordbotlist.com/bots/robertify/upvote", "Discord Bot List")
                    )
                    .queue();
            return false;
        }
        return true;
    }

    protected boolean djCheck(SlashCommandEvent event) {
        if (command == null)
            buildCommand();

        final Guild guild = event.getGuild();
        if (command.getDjOnly() && !GeneralUtils.hasPerms(guild, event.getMember(), Permission.ROBERTIFY_DJ)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, BotConstants.getInsufficientPermsMessage(Permission.ROBERTIFY_DJ)).build())
                    .setEphemeral(true)
                    .queue();
            return false;
        }
        return true;
    }

    protected boolean adminCheck(SlashCommandEvent event) {
        if (command == null)
            buildCommand();

        final Guild guild = event.getGuild();
        if (command.adminOnly && !GeneralUtils.hasPerms(guild, event.getMember(), Permission.ROBERTIFY_ADMIN)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, BotConstants.getInsufficientPermsMessage(Permission.ROBERTIFY_ADMIN)).build())
                    .setEphemeral(true)
                    .queue();
            return false;
        }
        return true;
    }

    protected boolean predicateCheck(SlashCommandEvent event) {
        if (command == null)
            buildCommand();
        if (command.getCheckPermission() == null)
            return true;
        return command.getCheckPermission().test(event);
    }

    protected boolean botPermsCheck(SlashCommandEvent event) {
        if (command == null)
            buildCommand();
        if (command.botRequiredPermissions.isEmpty())
            return true;

        final Guild guild = event.getGuild();
        final var self = guild.getSelfMember();
        if (!self.hasPermission(command.botRequiredPermissions)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "I do not have enough permissions to do this\n" +
                            "Please give my role the following permission(s):\n\n" +
                            "`"+GeneralUtils.listToString(command.botRequiredPermissions)+"`\n\n" +
                            "*For the recommended permissions please invite the bot using [this link](https://bit.ly/3DfaNNl)*").build())
                    .setEphemeral(false)
                    .queue();
            return false;
        }
        return true;
    }

    protected boolean botEmbedCheck(SlashCommandEvent event) {
        if (!event.getGuild().getSelfMember().hasPermission(net.dv8tion.jda.api.Permission.MESSAGE_EMBED_LINKS)) {
            event.reply("""
                                    ⚠️ I do not have permissions to send embeds!

                                    Please enable the `Embed Links` permission for my role in this channel in order for my commands to work!""")
                    .queue();
            return false;
        }
        return true;
    }

    protected boolean restrictedChannelCheck(SlashCommandEvent event) {
        final Guild guild = event.getGuild();
        final TogglesConfig togglesConfig = new TogglesConfig();
        final RestrictedChannelsConfig config = new RestrictedChannelsConfig();

        if (!togglesConfig.getToggle(guild, Toggles.RESTRICTED_TEXT_CHANNELS))
            return true;

        if (!config.isRestrictedChannel(guild.getIdLong(), event.getChannel().getIdLong(), RestrictedChannelsConfig.ChannelType.TEXT_CHANNEL)
            && !GeneralUtils.hasPerms(guild, event.getMember(), Permission.ROBERTIFY_ADMIN)) {

            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "This command cannot be used in this channel!\n\n" +
                    "Commands may only be used in:\n"
                    + GeneralUtils.listOfIDsToMentions(
                            config.getRestrictedChannels(guild.getIdLong(), RestrictedChannelsConfig.ChannelType.TEXT_CHANNEL),
                            GeneralUtils.Mentioner.CHANNEL
                    )).build())
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
        @Getter
        @NotNull
        private final String name;
        @Getter @Nullable
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
        private final Predicate<SlashCommandEvent> checkPermission;
        @NotNull @Getter
        private final Boolean djOnly;
        @NotNull @Getter
        private final Boolean adminOnly;
        private final boolean isPremium;
        @Getter
        private final boolean isPrivate;

        private Command(@NotNull String name, @Nullable String description, @NotNull List<CommandOption> options,
                        @NotNull List<SubCommandGroup> subCommandGroups, @NotNull List<SubCommand> subCommands, @Nullable Predicate<SlashCommandEvent> checkPermission,
                        @Nullable Boolean djOnly, @Nullable Boolean adminOnly, boolean isPremium, boolean isPrivate, List<Permission> requiredPermissions, List<net.dv8tion.jda.api.Permission> botRequiredPermissions) {
            this.name = name.toLowerCase();
            this.description = description;
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
        }

        public boolean permissionCheck(SlashCommandEvent e) {
            if (checkPermission == null)
                throw new NullPointerException("Can't perform permission check since a check predicate was not provided!");

            return checkPermission.test(e);
        }

        public static Command of(String name, String description, List<CommandOption> options, List<SubCommandGroup> subCommandGroups, List<SubCommand> subCommands, Predicate<SlashCommandEvent> checkPermission) {
            return new Command(name, description, options, subCommandGroups, subCommands, checkPermission, null, null, false, false, List.of(), List.of());
        }

        public static Command of(String name, String description, List<CommandOption> options, List<SubCommand> subCommands, Predicate<SlashCommandEvent> checkPermission) {
            return new Command(name, description, options, List.of(), subCommands, checkPermission, null, null, false, false, List.of(), List.of());
        }

        public static Command of(String name, String description, List<CommandOption> options,
                                                    List<SubCommand> subCommands, Predicate<SlashCommandEvent> checkPermission, boolean djOnly) {
            return new Command(name, description, options, List.of(), subCommands, checkPermission, djOnly, null, false, false, List.of(), List.of());
        }

        public static Command of(String name, String description, List<CommandOption> options,
                                                    List<SubCommand> subCommands, Predicate<SlashCommandEvent> checkPermission, boolean djOnly, boolean adminOnly) {
            return new Command(name, description, options, List.of(), subCommands, checkPermission, null, adminOnly, false, false, List.of(), List.of());
        }

        public static Command of(String name, String description, List<CommandOption> options, List<SubCommand> subCommands) {
            return new Command(name, description, options, List.of(), subCommands, null, null, null, false, false, List.of(), List.of());
        }

        public static Command of(String name, String description, List<CommandOption> options,
                                                    List<SubCommand> subCommands, boolean djOnly) {
            return new Command(name, description, options, List.of(), subCommands, null, djOnly, null, false, false, List.of(), List.of());
        }

        public static Command of(String name, String description, List<CommandOption> options,
                                                    List<SubCommand> subCommands, boolean djOnly, boolean adminOnly) {
            return new Command(name, description, options, List.of(), subCommands, null, null, adminOnly, false, false, List.of(), List.of());
        }

        public static Command of(String name, String description, List<CommandOption> options, Predicate<SlashCommandEvent> checkPermission) {
            return new Command(name, description, options, List.of(), List.of(), checkPermission, null, null, false, false, List.of(), List.of());
        }

        public static Command of(String name, String description, List<CommandOption> options,
                                                    Predicate<SlashCommandEvent> checkPermission, boolean djOnly) {
            return new Command(name, description, options, List.of(), List.of(), checkPermission, djOnly, null, false, false, List.of(), List.of());
        }

        public static Command of(String name, String description, List<CommandOption> options,
                                                    Predicate<SlashCommandEvent> checkPermission, boolean djOnly, boolean adminOnly) {
            return new Command(name, description, options, List.of(), List.of(), checkPermission, null, adminOnly, false, false, List.of(), List.of());
        }

        public static Command of(String name, String description, List<CommandOption> options) {
            return new Command(name, description, options, List.of(), List.of(), null, null, null, false, false, List.of(), List.of());
        }

        public static Command of(String name, String description, List<CommandOption> options, boolean djOnly) {
            return new Command(name, description, options, List.of(), List.of(), null, djOnly, null, false, false, List.of(), List.of());
        }

        public static Command of(String name, String description, List<CommandOption> options,
                                                    boolean djOnly, boolean adminOnly) {
            return new Command(name, description, options, List.of(), List.of(), null, null, adminOnly, false, false, List.of(), List.of());
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands, Predicate<SlashCommandEvent> checkPermission) {
            return new Command(name, description, List.of(), List.of(), subCommands, checkPermission, null, null, false, false, List.of(), List.of());
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands,
                                                           Predicate<SlashCommandEvent> checkPermission, boolean djOnly) {
            return new Command(name, description, List.of(), List.of(), subCommands, checkPermission, djOnly, null, false, false, List.of(), List.of());
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands,
                                                           Predicate<SlashCommandEvent> checkPermission, boolean djOnly, boolean adminOnly) {
            return new Command(name, description, List.of(), List.of(), subCommands, checkPermission, null, adminOnly, false, false, List.of(), List.of());
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands) {
            return new Command(name, description, List.of(), List.of(), subCommands, null, null, null, false, false, List.of(), List.of());
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands, List<SubCommandGroup> subCommandGroups) {
            return new Command(name, description, List.of(), subCommandGroups, subCommands, null, null, null, false, false, List.of(), List.of());
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands,
                                                           boolean djOnly) {
            return new Command(name, description, List.of(), List.of(), subCommands, null, djOnly, null, false, false, List.of(), List.of());
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands,
                                                           boolean djOnly, boolean adminOnly) {
            return new Command(name, description, List.of(), List.of(), subCommands, null, null, adminOnly, false, false, List.of(), List.of());
        }

        public static Command of(String name, String description, Predicate<SlashCommandEvent> checkPermission) {
            return new Command(name, description, List.of(), List.of(), List.of(), checkPermission, null, null, false, false, List.of(), List.of());
        }

        public static Command of(String name, String description,
                                                    Predicate<SlashCommandEvent> checkPermission, boolean djOnly) {
            return new Command(name, description, List.of(), List.of(), List.of(), checkPermission, djOnly, null, false, false, List.of(), List.of());
        }

        public static Command of(String name, String description,
                                                    Predicate<SlashCommandEvent> checkPermission, boolean djOnly, boolean adminOnly) {
            return new Command(name, description, List.of(), List.of(), List.of(), checkPermission, null, adminOnly, false, false, List.of(), List.of());
        }

        public static Command of(String name, String description) {
            return new Command(name, description, List.of(), List.of(), List.of(), null, null, null, false, false, List.of(), List.of());
        }

        public static Command of(String name, String description, boolean djOnly) {
            return new Command(name, description, List.of(), List.of(), List.of(), null, djOnly, null, false, false, List.of(), List.of());
        }

        public static Command of(String name, String description, boolean djOnly, boolean adminOnly) {
            return new Command(name, description, List.of(), List.of(), List.of(), null, null, adminOnly, false, false, List.of(), List.of());
        }
    }

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

        public SubcommandGroupData build() throws InvalidBuilderException {

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
        private Predicate<SlashCommandEvent> permissionCheck;
        private boolean djOnly, adminOnly, isPremium, isPrivate;

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

        public Builder setPermissionCheck(Predicate<SlashCommandEvent> predicate) {
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
                final TogglesConfig config = new TogglesConfig();

                if (!config.isDJToggleSet(e.getGuild(), e.getName()))
                    return true;

                if (config.getDJToggle(e.getGuild(), new SlashCommandManager().getCommand(e.getName())))
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

        public Builder setDevCommand() {
            this.isPrivate = true;
            this.permissionCheck = e -> BotInfoCache.getInstance().isDeveloper(e.getUser().getIdLong());
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
                    botRequiredPermissions
            );
        }
    }
}
