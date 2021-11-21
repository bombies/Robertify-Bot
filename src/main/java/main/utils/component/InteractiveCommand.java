package main.utils.component;

import lombok.Getter;
import lombok.Setter;
import main.utils.database.BotUtils;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public abstract class InteractiveCommand extends ListenerAdapter {
    @Setter
    @Getter
    private static InteractionCommand interactionCommand;

    public abstract void initCommand();
    public abstract void initCommand(Guild g);

    public static void upsertCommand() {
        interactionCommand.pushToAllGuilds();
    }

    public static void upsertCommand(Guild g) {
        interactionCommand.pushToGuild(g);
    }

    public SelectionDialogue getSelectionDialogue(String name) {
        return interactionCommand.getSelectionDialogues().get(name);
    }

    public static class InteractionCommand {
        @Getter
        private final Command command;
        @Getter
        private final HashMap<String, SelectionDialogue> selectionDialogues;

        private InteractionCommand(@NotNull Command command, @NotNull HashMap<String, SelectionDialogue> selectionDialogues) {
            this.command = command;
            this.selectionDialogues = selectionDialogues;
        }

        public static InteractionBuilder create() {
            return new InteractionBuilder();
        }

        public SelectionMenu getSelectionMenu(String menu) throws InteractionBuilderException {
            if (selectionDialogues.isEmpty())
                throw new InteractionBuilderException("There are no selection dialogues to build!");

            var selectionDialogue = selectionDialogues.get(menu);

            if (selectionDialogue == null)
                throw new NullPointerException("There is no dialogue with the name \""+menu+"\"");

            SelectionMenu.Builder builder = SelectionMenu.create(selectionDialogue.getName())
                    .setPlaceholder(selectionDialogue.getPlaceholder())
                    .setRequiredRange(selectionDialogue.range.getLeft(), selectionDialogue.range.getLeft());

            for (Triple<String, String, String> val : selectionDialogue.getOptions())
                if (val.getRight() == null)
                    builder.addOption(val.getLeft(), val.getMiddle());
                else
                    builder.addOption(val.getLeft(), val.getMiddle(), Emoji.fromUnicode(val.getRight()));

            return builder.build();
        }

        public void pushToAllGuilds() {
            addCommandToAllGuilds(command);
        }

        public void pushToGuild(Guild g) {
            addCommandToSpecificGuild(g, command);
        }

        private void addCommandToAllGuilds(Command command) {
            for (Guild g : new BotUtils().getGuilds())
                addCommandToSpecificGuild(g, command);
        }

        private void addCommandToSpecificGuild(Guild g, Command command) {
            // Initial request builder
            CommandCreateAction commandCreateAction = g.upsertCommand(command.getName(), command.getDescription());

            // Adding subcommands
            if (!command.getSubCommands().isEmpty()) {
                for (SubCommand subCommand : command.getSubCommands()) {
                    var subCommandData = new SubcommandData(subCommand.getName(), subCommand.getDescription());

                    // Adding options for subcommands
                    for (CommandOption options : subCommand.getOptions())
                        subCommandData.addOption(options.getType(), options.getName(), options.getDescription(), options.isRequired());

                    commandCreateAction = commandCreateAction.addSubcommands(subCommandData);
                }
            } else {
                // Adding options for the main command
                for (CommandOption options : command.getOptions())
                    commandCreateAction = commandCreateAction.addOption(options.getType(), options.getName(), options.getDescription(), options.isRequired());
            }

            commandCreateAction.queue();
        }
    }

    public static class InteractionBuilder {
        private Command command;
        private HashMap<String, SelectionDialogue> selectionDialogues = new HashMap<>();

        public InteractionBuilder setCommand(@NotNull Command command) {
            this.command = command;
            return this;
        }

        public CommandBuilder buildCommand() {
            return new CommandBuilder(this);
        }

        public InteractionBuilder addSelectionDialogue(@NotNull SelectionDialogue selectionDialogue) {
            selectionDialogues.put(selectionDialogue.getName(), selectionDialogue);
            return this;
        }

        public SelectionDialogueBuilder buildSelectionDialogue() {
            return new SelectionDialogueBuilder(this);
        }

        public InteractionCommand build() {
            return new InteractionCommand(command, selectionDialogues);
        }
    }

    public static class CommandBuilder {
        private String name;
        @Nullable
        private String description;
        @NotNull
        private final List<CommandOption> commandOptions = new ArrayList<>();
        @NotNull
        private final List<SubCommand> subCommands = new ArrayList<>();
        @Nullable
        private BiPredicate<Member, Guild> checkPermission = null;
        private final InteractionBuilder builder;

        public CommandBuilder(InteractionBuilder builder) {
            this.builder = builder;
        }

        public CommandBuilder setName(@NotNull String name) {
            this.name = name;
            return this;
        }

        public CommandBuilder setDescription(@NotNull String description) {
            this.description = description;
            return this;
        }

        public CommandBuilder addSubCommand(@NotNull SubCommand command) {
            subCommands.add(command);
            return this;
        }

        public SubCommandBuilder buildSubCommand() {
            return new SubCommandBuilder(this);
        }

        public CommandBuilder addOption(@NotNull InteractiveCommand.CommandOption option) {
            commandOptions.add(option);
            return this;
        }

        public CommandBuilder setPermissionCheck(BiPredicate<Member, Guild> predicate) {
            checkPermission = predicate;
            return this;
        }

        public InteractionBuilder build() throws InvalidBuilderException {
            if (name == null)
                throw new InvalidBuilderException("You must provide a name for the command!");

            if (!subCommands.isEmpty() && !commandOptions.isEmpty())
                throw new InvalidBuilderException("You can't have options for the main command if you have subcommands!");
            else if (subCommands.isEmpty() && commandOptions.isEmpty())
                throw new InvalidBuilderException("You can't have both main command options and subcommands empty!");


            return this.builder.setCommand(new Command(name, description, commandOptions, subCommands, checkPermission));
        }
    }

    public static class Command {
        @Getter @NotNull
        private final String name;
        @Getter @Nullable
        private final String description;
        @Getter @NotNull
        private final List<CommandOption> options;
        @Getter @NotNull
        private final List<SubCommand> subCommands;
        @Nullable
        private final BiPredicate<Member, Guild> checkPermission;

        private Command(@NotNull String name, @Nullable String description, @NotNull List<CommandOption> options, @NotNull List<SubCommand> subCommands, @Nullable BiPredicate<Member, Guild> checkPermission) {
            this.name = name.toLowerCase();
            this.description = description;
            this.options = options;
            this.subCommands = subCommands;
            this.checkPermission = checkPermission;
        }

        public boolean permissionCheck(Member user, Guild guild) {
            if (checkPermission == null)
                throw new NullPointerException("Can't perform permission check since a check predicate was not provided!");

            return checkPermission.test(user, guild);
        }

        public static Command of(String name, String description, List<CommandOption> options, List<SubCommand> subCommands, BiPredicate<Member, Guild> checkPermission) {
            return new Command(name, description, options, subCommands, checkPermission);
        }

        public static Command of(String name, String description, List<CommandOption> options, List<SubCommand> subCommands) {
            return new Command(name, description, options, subCommands, null);
        }

        public static Command of(String name, String description, List<CommandOption> options, BiPredicate<Member, Guild> checkPermission) {
            return new Command(name, description, options, List.of(), checkPermission);
        }

        public static Command of(String name, String description, List<CommandOption> options) {
            return new Command(name, description, options, List.of(), null);
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands, BiPredicate<Member, Guild> checkPermission) {
            return new Command(name, description, List.of(), subCommands, checkPermission);
        }

        public static Command ofWithSub(String name, String description, List<SubCommand> subCommands) {
            return new Command(name, description, List.of(), subCommands, null);
        }

        public static Command of(String name, String description, BiPredicate<Member, Guild> checkPermission) {
            return new Command(name, description, List.of(), List.of(), checkPermission);
        }

        public static Command of(String name, String description) {
            return new Command(name, description, List.of(), List.of(), null);
        }
    }

    public static class SubCommandBuilder {
        private String name;
        private String description;
        private List<CommandOption> options = new ArrayList<>();
        private final CommandBuilder builder;

        public SubCommandBuilder(CommandBuilder builder) {
            this.builder = builder;
        }

        public SubCommandBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public SubCommandBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public SubCommandBuilder addOption(CommandOption option) {
            options.add(option);
            return this;
        }

        public SubCommandBuilder setOptions(List<CommandOption> options) {
            this.options = options;
            return this;
        }

        public CommandBuilder build() {
            return builder.addSubCommand(new SubCommand(name, description, options));
        }
    }

    public static class SubCommand {
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
    }

    public static class CommandOption {
        @Getter
        private final OptionType type;
        @Getter
        private final String name;
        @Getter
        private final String description;
        @Getter
        private final boolean required;

        private CommandOption(OptionType type, String name, String description, boolean required) {
            this.type = type;
            this.name = name.toLowerCase();
            this.description = description;
            this.required = required;
        }

        public static CommandOption of(OptionType type, String name, String description, boolean required) {
            return new CommandOption(type, name, description, required);
        }
    }

    public static class SelectionDialogueBuilder {
        private String name;
        private String placeholder;
        private Pair<Integer, Integer> range;
        private final List<Triple<String, String, String>> options = new ArrayList<>();
        private Predicate<SelectionMenuEvent> predicate;
        private final InteractionBuilder builder;

        public SelectionDialogueBuilder(InteractionBuilder builder) {
            this.builder = builder;
        }

        public SelectionDialogueBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public SelectionDialogueBuilder setPlaceHolder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public SelectionDialogueBuilder setRange(int min, int max) {
            this.range = Pair.of(min, max);
            return this;
        }

        public SelectionDialogueBuilder addOption(String label, String value) {
            options.add(Triple.of(label.toLowerCase(), value.toLowerCase(), null));
            return this;
        }

        public SelectionDialogueBuilder addOption(String label, String value, String emoji) {
            options.add(Triple.of(label.toLowerCase(), value.toLowerCase(), emoji));
            return this;
        }

        public SelectionDialogueBuilder setPermission(Predicate<SelectionMenuEvent> predicate) {
            this.predicate = predicate;
            return this;
        }

        public InteractionBuilder build() throws InvalidBuilderException {
            if (name == null)
                throw new InvalidBuilderException("The name of the menu cannot be null!");

            if (placeholder == null)
                throw new InvalidBuilderException("The placeholder for the menu cannot be null!");

            if (range == null)
                throw new InvalidBuilderException("The range of options can't be null!");

            if (range.getLeft() > range.getRight())
                throw new InvalidBuilderException("The minimum value of the range can't be more than the maximum value");

            if (options.isEmpty())
                throw new InvalidBuilderException("The options list can't be empty!");


            return this.builder.addSelectionDialogue(new SelectionDialogue(name, placeholder, range, options, predicate));
        }
    }

    public static class SelectionDialogue {
        @Getter @NotNull
        private final String name;
        @Getter @NotNull
        private final String placeholder;
        @Getter @NotNull
        private final Pair<Integer, Integer> range;
        @Getter @NotNull
        private final List<Triple<String, String, String>> options;
        @Nullable
        private final Predicate<SelectionMenuEvent> permissionCheck;

        private SelectionDialogue(@NotNull String name, @NotNull String placeholder, @NotNull Pair<Integer, Integer> range, @NotNull List<Triple<String,String, String>> options, @Nullable Predicate<SelectionMenuEvent> permissionCheck) {
            this.name = name.toLowerCase();
            this.placeholder = placeholder;
            this.range = range;
            this.options = options;
            this.permissionCheck = permissionCheck;
        }

        public boolean checkPermission(SelectionMenuEvent e) {
            if (permissionCheck == null)
                throw new NullPointerException("There is no permission to check!");
            return permissionCheck.test(e);
        }

        /**
         *
         * @param name Name of the menu to be used as an identifier
         * @param placeholder The text that is to show up on the menu when there is nothing selected
         * @param range The range of values that will be allowed to be selected. Pair(Min, Max)
         * @param options This of pair of options to be presented List(Pair(Label, Value))
         * @return A new fancy selection menu
         */
        public static SelectionDialogue of(String name, String placeholder, Pair<Integer, Integer> range, List<Triple<String, String, String>> options) {
            return new SelectionDialogue(name, placeholder, range, options, null);
        }

        /**
         *
         * @param name Name of the menu to be used as an identifier
         * @param placeholder The text that is to show up on the menu when there is nothing selected
         * @param range The range of values that will be allowed to be selected. Pair(Min, Max)
         * @param options This of pair of options to be presented List(Pair(Label, Value))
         * @param permissionCheck The check that can be performed when a user interacts with the selection menu
         * @return A new fancy selection menu
         */
        public static SelectionDialogue of(String name, String placeholder, Pair<Integer, Integer> range, List<Triple<String, String, String>> options, Predicate<SelectionMenuEvent> permissionCheck) {
            return new SelectionDialogue(name, placeholder, range, options, permissionCheck);
        }
    }
}
