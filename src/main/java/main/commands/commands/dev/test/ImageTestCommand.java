package main.commands.commands.dev.test;

import main.commands.CommandContext;
import main.commands.ITestCommand;
import main.utils.ImageBuilder;

import javax.script.ScriptException;
import java.awt.*;
import java.io.File;

public class ImageTestCommand implements ITestCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        final var args = ctx.getArgs();
        final var msg = ctx.getMessage();

        if (args.isEmpty()) {
            msg.reply("Provide args idiot").queue();
            return;
        }

        File file = ImageBuilder.create(250, 75)
                .setBackground(Color.BLUE)
                .addText(String.join(" ", args), Color.WHITE)
                .build("test.png");

        ctx.getMessage().reply(file).queue(success -> file.delete());
    }

    @Override
    public String getName() {
        return "imgtest";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }
}
