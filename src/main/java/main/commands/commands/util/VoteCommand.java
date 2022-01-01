package main.commands.commands.util;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.component.InteractiveCommand;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;

public class VoteCommand extends InteractiveCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        ctx.getMessage().replyEmbeds(EmbedUtils.embedMessage("Thank you for taking the interest in supporting us!\n" +
                        "You may press on each of the buttons below to vote for us.")
                        .build())
                .setActionRow(
                        Button.of(ButtonStyle.LINK, "https://discordbotlist.com/bots/robertify/upvote", "Link #1"),
                        Button.of(ButtonStyle.LINK, "https://discordextremelist.xyz/en-US/bots/893558050504466482", "Link #2")
                )
                .queue();
    }

    @Override
    public String getName() {
        return "vote";
    }

    @Override
    public String getHelp(String prefix) {
        return "Do you like Robertify and want to help us share it with more users? Do us the favour of voting for us! " +
                "It would really help us in growing our reach. 💖";
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

    public InteractionCommand getCommand() {
        return InteractionCommand.create()
                .setCommand(Command.of(
                    getName(),
                    "Want to support us? Help spread our reach by voting for us!"
                )).build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(getName())) return;

        event.replyEmbeds(EmbedUtils.embedMessage("Thank you for taking the interest in supporting us!\n" +
                        "You may press on each of the buttons below to vote for us.")
                .build())
                .addActionRow(
                        Button.of(ButtonStyle.LINK, "https://discordbotlist.com/bots/robertify/upvote", "Link #1"),
                        Button.of(ButtonStyle.LINK, "https://discordextremelist.xyz/en-US/bots/893558050504466482", "Link #2")
                )
                .queue();
    }
}
