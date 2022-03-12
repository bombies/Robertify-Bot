package main.commands.prefixcommands.dev.test;

import lombok.SneakyThrows;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ITestCommand;
import main.utils.ImageBuilder;

import javax.imageio.ImageIO;
import javax.script.ScriptException;
import java.awt.*;
import java.io.File;

public class ImageTestCommand implements ITestCommand {
    @Override @SneakyThrows
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        final var args = ctx.getArgs();
        final var msg = ctx.getMessage();

        if (args.isEmpty()) {
            msg.reply("Provide args idiot").queue();
            return;
        }

        File file = ImageBuilder.create(934, 282)
                .setBackground(ImageIO.read(new File("static/Card.png")))
                .addText(
                        String.join(" ", args),
                        Color.WHITE,
                        new Font("Times New Roman", Font.BOLD, 50),
                        934/2, 282/2
                )
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
