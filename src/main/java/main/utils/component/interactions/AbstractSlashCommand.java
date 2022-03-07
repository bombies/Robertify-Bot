package main.utils.component.interactions;

import lombok.Getter;
import lombok.SneakyThrows;
import main.utils.component.AbstractInteraction;
import main.utils.component.InvalidBuilderException;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public abstract class AbstractSlashCommand extends AbstractInteraction {
    private Command command = null;

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

        commandCreateAction.queueAfter(1, TimeUnit.SECONDS, null, new ErrorHandler()
                .handle(ErrorResponse.MISSING_ACCESS, e -> {}));
    }

    protected void setCommand(Command command) {
        this.command = command;
    }

    protected Command getCommand() {
        return command;
    }

    protected boolean nameCheck(SlashCommandEvent event) {
        if (command == null)
            buildCommand();
        return command.getName().equals(event.getName());
    }

    protected abstract void buildCommand();

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
        @Nullable @Getter
        private final Predicate<SlashCommandEvent> checkPermission;
        @Nullable @Getter
        private final Boolean djOnly;
        @Nullable @Getter
        private final Boolean adminOnly;

        private Command(@NotNull String name, @Nullable String description, @NotNull List<CommandOption> options,
                        @NotNull List<SubCommandGroup> subCommandGroups, @NotNull List<SubCommand> subCommands, @Nullable Predicate<SlashCommandEvent> checkPermission,
                        @Nullable Boolean djOnly, @Nullable Boolean adminOnly) {
            this.name = name.toLowerCase();
            this.description = description;
            this.options = options;
            this.subCommandGroups = subCommandGroups;
            this.subCommands = subCommands;
            this.checkPermission = checkPermission;
            this.djOnly = djOnly;
            this.adminOnly = adminOnly;
        }

        public boolean permissionCheck(SlashCommandEvent e) {
            if (checkPermission == null)
                throw new NullPointerException("Can't perform permission check since a check predicate was not provided!");

            return checkPermission.test(e);
        }

        public static Command of(String name, String description, List<CommandOption> options, List<SubCommandGroup> subCommandGroups, List<SubCommand> subCommands, Predicate<SlashCommandEvent> checkPermission) {
            return new Command(name, description, options, subCommandGroups, subCommands, checkPermission, null, null);
        }

        public static Command of(String name, String description, List<CommandOption> options, List<SubCommand> subCommands, Predicate<SlashCommandEvent> checkPermission) {
            return new Command(name, description, options, List.of(), subCommands, checkPermission, null, null);
        }

        public static Command of(String name, String description, List<CommandOption> options,
                                                    List<SubCommand> subCommands, Predicate<SlashCommandEvent> checkPermission, boolean djOnly) {
            return new Command(name, description, options, List.of(), subCommands, checkPermission, djOnly, null);
        }

        public static Command of(String name, String description, List<CommandOption> options,
                                                    List<SubCommand> subCommands, Predicate<SlashCommandEvent> checkPermission, boolean djOnly, boolean adminOnly) {
            return new Command(name, description, options, List.of(), subCommands, checkPermission, null, adminOnly);
        }

        public static Command of(String name, String description, List<CommandOption> options, List<SubCommand> subCommands) {
            return new Command(name, description, options, List.of(), subCommands, null, null, null);
        }

        public static Command of(String name, String description, List<CommandOption> options,
                                                    List<SubCommand> subCommands, boolean djOnly) {
            return new Command(name, description, options, List.of(), subCommands, null, djOnly, null);
        }

        public static Command of(String name, String description, List<CommandOption> options,
                                                    List<SubCommand> subCommands, boolean djOnly, boolean adminOnly) {
            return new Command(name, description, options, List.of(), subCommands, null, null, adminOnly);
        }

        public static Command of(String name, String description, List<CommandOption> options, Predicate<SlashCommandEvent> checkPermission) {
            return new Command(name, description, options, List.of(), List.of(), checkPermission, null, null);
        }

        public static Command of(String name, String description, List<CommandOption> options,
                                                    Predicate<SlashCommandEvent> checkPermission, boolean djOnly) {
            return new Command(name, description, options, List.of(), List.of(), checkPermission, djOnly, null);
        }

        public static Command of(String name, String description, List<CommandOption> options,
                                                    Predicate<SlashCommandEvent> checkPermission, boolean djOnly, boolean adminOnly) {
            return new Command(name, description, options, List.of(), List.of(), checkPermission, null, adminOnly);
        }

        public static Command of(String name, String description, List<CommandOption> options) {
            return new Command(name, description, options, List.of(), List.of(), null, null, null);
        }

        public static Command of(String name, String description, List<CommandOption> options, boolean djOnly) {
            return new Command(name, description, options, List.of(), List.of(), null, djOnly, null);
        }

        public static Command of(String name, String description, List<CommandOption> options,
                                                    boolean djOnly, boolean adminOnly) {
            return new Command(name, description, options, List.of(), List.of(), null, null, adminOnly);
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands, Predicate<SlashCommandEvent> checkPermission) {
            return new Command(name, description, List.of(), List.of(), subCommands, checkPermission, null, null);
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands,
                                                           Predicate<SlashCommandEvent> checkPermission, boolean djOnly) {
            return new Command(name, description, List.of(), List.of(), subCommands, checkPermission, djOnly, null);
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands,
                                                           Predicate<SlashCommandEvent> checkPermission, boolean djOnly, boolean adminOnly) {
            return new Command(name, description, List.of(), List.of(), subCommands, checkPermission, null, adminOnly);
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands) {
            return new Command(name, description, List.of(), List.of(), subCommands, null, null, null);
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands, List<SubCommandGroup> subCommandGroups) {
            return new Command(name, description, List.of(), subCommandGroups, subCommands, null, null, null);
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands,
                                                           boolean djOnly) {
            return new Command(name, description, List.of(), List.of(), subCommands, null, djOnly, null);
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands,
                                                           boolean djOnly, boolean adminOnly) {
            return new Command(name, description, List.of(), List.of(), subCommands, null, null, adminOnly);
        }

        public static Command of(String name, String description, Predicate<SlashCommandEvent> checkPermission) {
            return new Command(name, description, List.of(), List.of(), List.of(), checkPermission, null, null);
        }

        public static Command of(String name, String description,
                                                    Predicate<SlashCommandEvent> checkPermission, boolean djOnly) {
            return new Command(name, description, List.of(), List.of(), List.of(), checkPermission, djOnly, null);
        }

        public static Command of(String name, String description,
                                                    Predicate<SlashCommandEvent> checkPermission, boolean djOnly, boolean adminOnly) {
            return new Command(name, description, List.of(), List.of(), List.of(), checkPermission, null, adminOnly);
        }

        public static Command of(String name, String description) {
            return new Command(name, description, List.of(), List.of(), List.of(), null, null, null);
        }

        public static Command of(String name, String description, boolean djOnly) {
            return new Command(name, description, List.of(), List.of(), List.of(), null, djOnly, null);
        }

        public static Command of(String name, String description, boolean djOnly, boolean adminOnly) {
            return new Command(name, description, List.of(), List.of(), List.of(), null, null, adminOnly);
        }
    }

    protected static class SubCommand {
        @NotNull @Getter
        private final String name;
        @Nullable @Getter
        private final String description;
        @NotNull @Getter
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

                for (var option : command.getOptions())
                    subcommandData.addOptions(new OptionData(
                            option.getType(),
                            option.getName(),
                            option.getDescription(),
                            option.isRequired()
                    ));

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
        private Predicate<SlashCommandEvent> permissionCheck;
        private boolean djOnly, adminOnly;

        private Builder() {
            this.options = new ArrayList<>();
            this.subCommands = new ArrayList<>();
            this.subCommandGroups = new ArrayList<>();
            this.djOnly = false;
            this.adminOnly = false;
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

        public Builder setDJOnly() throws InvalidBuilderException {
            if (adminOnly)
                throw new InvalidBuilderException("DJ-only mode for this command can't be set to true when it's already admin-only");
            djOnly = true;
            return this;
        }

        public Builder setAdminOnly() {
            if (djOnly)
                djOnly = false;
            adminOnly = true;
            return this;
        }

        public Command build() throws InvalidBuilderException {
           if (name == null)
               throw new InvalidBuilderException("The name of the command can't be null!");
           if (name.isBlank())
               throw new InvalidBuilderException("The name of the command can't be empty!");
            if (description == null)
                throw new InvalidBuilderException("The description of the command can't be null!");
            if (description.isBlank())
                throw new InvalidBuilderException("The description of the command can't be empty!");

            return new Command(
                    name,
                    description,
                    options,
                    subCommandGroups,
                    subCommands,
                    permissionCheck,
                    djOnly,
                    adminOnly
            );
        }
    }
}
