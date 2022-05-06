//package main.utils.component.interactions.tests;
//
//import main.utils.component.interactions.AbstractContextCommand;
//import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
//import net.dv8tion.jda.api.interactions.commands.Command;
//import org.jetbrains.annotations.NotNull;
//
//public class ContextCommandTest extends AbstractContextCommand {
//    @Override
//    protected void buildCommand() {
//        setCommand(
//                getBuilder()
//                        .setName("test")
//                        .setType(Command.Type.MESSAGE)
//                        .build()
//        );
//    }
//
//    @Override
//    public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent event) {
//        if (!event.isFromGuild()) return;
//        event.reply("Context commands work!").queue();
//    }
//}
