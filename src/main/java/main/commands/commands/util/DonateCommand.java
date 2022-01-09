package main.commands.commands.util;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.InteractiveCommand;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;

public class DonateCommand extends InteractiveCommand implements ICommand {
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
                        Button.of(ButtonStyle.LINK, "https://www.patreon.com/robertify", "Patreon"),
                        Button.of(ButtonStyle.LINK, "https://donatebot.io/checkout/922856265821061130", "Donate Bot")
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
    public void initCommand() {
        setInteractionCommand(getCommand());
        upsertCommand();
    }

    @Override
    public void initCommand(Guild g) {
        setInteractionCommand(getCommand());
        upsertCommand(g);
    }

    private InteractionCommand getCommand() {
        return InteractionCommand.create()
                .setCommand(Command.of(
                        getName(),
                        "Help keep Robertify online!"
                ))
                .build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(getName())) return;

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
