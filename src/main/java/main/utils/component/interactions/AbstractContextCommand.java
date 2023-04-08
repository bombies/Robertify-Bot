package main.utils.component.interactions;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import main.main.Robertify;
import main.utils.component.AbstractInteraction;
import main.utils.component.InvalidBuilderException;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

@Slf4j
public abstract class AbstractContextCommand extends AbstractInteraction {

    @Setter
    @Getter
    private ContextCommand command = null;

    public String getName() {
        if (command == null)
            buildAndSetCommand();
        return command.name;
    }

    public Command.Type getType() {
        if (command == null)
            buildAndSetCommand();
        return command.type;
    }

    protected abstract ContextCommandData buildCommand();

    protected ContextCommandData.ContextCommandDataBuilder getBuilder() {
        return ContextCommandData.builder();
    }

    private void buildAndSetCommand() {
        final var contextCmdData = buildCommand();
        if (contextCmdData.type() == null) {
            log.error("The context command type must not be null!");
            return;
        }
        if (contextCmdData.name() == null) {
            log.error("The context command name must not be null!");
            return;
        }
        this.command = new ContextCommand(contextCmdData.type, contextCmdData.name, contextCmdData.guildOnly);
    }

    public CommandData getCommandData() {
        return Commands.context(command.type, command.name)
                .setGuildOnly(command.guildOnly);
    }

    public void load(Guild g) {
        buildAndSetCommand();
        g.upsertCommand(Commands.context(command.type, command.name)).queue();
    }

    public void load() {
        buildAndSetCommand();
        Robertify.getShardManager().getShards()
                .forEach(shard -> shard.upsertCommand(getCommandData()).queue());
    }

    private boolean nameCheck(GenericCommandInteractionEvent event) {
        if (command == null)
            buildAndSetCommand();
        return event.getName().equals(command.name);
    }

    protected boolean checks(GenericCommandInteractionEvent event) {
        return nameCheck(event);
    }

    protected static class ContextCommand {
        @Getter
        @NotNull
        private final Command.Type type;
        @Getter
        @NotNull
        private final String name;
        @Getter
        private final boolean guildOnly;

        private ContextCommand(@NotNull Command.Type type, @NotNull String name, boolean guildOnly) {
            this.type = type;
            this.name = name;
            this.guildOnly = guildOnly;
        }
    }

    @Builder
    public record ContextCommandData(Command.Type type, String name, boolean guildOnly) {
    }

//    public static class ContextCommandData {
//        Command.Type type;
//        String name;
//        boolean guildOnly;
//
//        @SneakyThrows
//        public ContextCommand build() {
//            if (type == null)
//                throw new InvalidBuilderException("The context command type must not be null!");
//            if (name == null)
//                throw new InvalidBuilderException("The context command name must not be null!");
//            return new ContextCommand(type, name);
//        }
//    }
}
