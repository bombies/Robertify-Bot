package main.commands.slashcommands.commands.util;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;

public class DonateCommand extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {

        ctx.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(
                ctx.getGuild(),
                "Donations",
                "Thank you for taking the interesting in donating to us! This simple gesture " +
                        "means a lot. Donating to Robertify would help maintain upkeep.\n" +
                        "A $10 donation is equivalent to 1 more month Robertify stays online. 🙂"
        ).build())
                .setActionRow(
                        Button.of(ButtonStyle.LINK, "https://www.patreon.com/robertify", "Patreon")
                )
                .queue();
    }

    @Override
    public String getName() {
        return "donate";
    }

    @Override
    public String getHelp(String prefix) {
        return "Want to help keep Robertify online? Donate using these links!\n" +
                "1 $10 donation is equivalent to 1 more month Robertify gets to stay online!";
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("donate")
                        .setDescription("Help keep Robertify online!")
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Want to help keep Robertify online? Donate using these links!\n" +
                "1 $10 donation is equivalent to 1 more month Robertify gets to stay online!";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;

        event.replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(
                        event.getGuild(),
                        "Donations",
                        "Thank you for taking the interesting in donating to us! This simple gesture " +
                                "means a lot. Donating to Robertify would help maintain upkeep.\n" +
                                "A $10 donation is equivalent to 1 more month Robertify stays online. 🙂"
                ).build())
                .addActionRow(
                        Button.of(ButtonStyle.LINK, "https://www.patreon.com/robertify", "Patreon"),
                        Button.of(ButtonStyle.LINK, "https://donatebot.io/checkout/922856265821061130", "Donate Bot")
                )
                .queue();
    }
}
