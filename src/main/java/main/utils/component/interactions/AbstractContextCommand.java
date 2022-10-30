package main.utils.component.interactions;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import main.utils.component.AbstractInteraction;
import main.utils.component.InvalidBuilderException;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractContextCommand extends AbstractInteraction {

    @Setter @Getter
    private ContextCommand command = null;

    public String getName() {
        if (command == null)
            buildCommand();
        return command.name;
    }

    public Command.Type getType() {
        if (command == null)
            buildCommand();
        return command.type;
    }

    protected abstract void buildCommand();
    protected Builder getBuilder() {
        return new Builder();
    }

    public CommandData getCommandData() {
        buildCommand();
        return Commands.context(command.type, command.name);
    }

    public void loadCommand(Guild g) {
        buildCommand();

        g.upsertCommand(Commands.context(command.type, command.name))
                .queue();
    }

    protected static class ContextCommand {
        @Getter @NotNull
        private final Command.Type type;
        @Getter @NotNull
        private final String name;

        private ContextCommand(@NotNull Command.Type type, @NotNull String name) {
            this.type = type;
            this.name = name;
        }
    }

    protected static class Builder {
        private Command.Type type;
        private String name;

        private Builder() {}

        public Builder setType(Command.Type type) {
            this.type = type;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        @SneakyThrows
        public ContextCommand build() {
            if (type == null)
                throw new InvalidBuilderException("The context command type must not be null!");
            if (name == null)
                throw new InvalidBuilderException("The context command name must not be null!");
            return new ContextCommand(type, name);
        }
    }
}
