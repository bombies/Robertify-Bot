//package main.commands.contextcommands;
//
//import lombok.Getter;
//import main.utils.component.interactions.AbstractContextCommand;
//import main.utils.component.interactions.AbstractSlashCommand;
//import main.utils.component.interactions.tests.ContextCommandTest;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//public class ContextCommandManager {
//
//    @Getter
//    private final List<AbstractContextCommand> commands = new ArrayList<>();
//
//    public ContextCommandManager() {
//        addCommands(
//                new ContextCommandTest()
//        );
//    }
//
//    private void addCommands(AbstractContextCommand... commands) {
//        this.commands.addAll(Arrays.asList(commands));
//    }
//
//    public AbstractContextCommand getCommand(String name) {
//        return getCommands().stream()
//                .filter(cmd -> cmd.getName().equalsIgnoreCase(name))
//                .findFirst()
//                .orElse(null);
//    }
//}
