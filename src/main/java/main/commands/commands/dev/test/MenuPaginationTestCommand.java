package main.commands.commands.dev.test;

import main.commands.CommandContext;
import main.commands.ITestCommand;
import main.utils.component.builders.selectionmenu.SelectionMenuOption;
import main.utils.pagination.Pages;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;

public class MenuPaginationTestCommand extends ListenerAdapter implements ITestCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        List<SelectionMenuOption> options = new ArrayList<>();

        for (int i = 0; i < 30; i++)
            options.add(SelectionMenuOption.of("Test " + i, "Test:" + i));

        ctx.getMessage().reply("Menu Pagination Test")
                .queue(success -> Pages.paginateMenu(success, options));
    }

    @Override
    public String getName() {
        return "menupagination";
    }

    @Override
    public List<String> getAliases() {
        return List.of("mpt");
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        final var selectedOption = event.getSelectedOptions().get(0).getValue();

        if (!selectedOption.startsWith("Test")) return;

        event.reply("You clicked: Test " + selectedOption.split(":")[1]).queue();
    }
}
