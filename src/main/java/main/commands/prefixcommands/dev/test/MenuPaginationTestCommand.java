package main.commands.prefixcommands.dev.test;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ITestCommand;
import main.utils.component.interactions.selectionmenu.SelectMenuOption;
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

        List<SelectMenuOption> options = new ArrayList<>();

        for (int i = 0; i < 30; i++)
            options.add(SelectMenuOption.of("Test " + i, "Test:" + i));

        ctx.getMessage().reply("Menu Pagination Test")
                .queue(success -> Pages.paginateMenu(ctx.getAuthor(), success, options));
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
}
