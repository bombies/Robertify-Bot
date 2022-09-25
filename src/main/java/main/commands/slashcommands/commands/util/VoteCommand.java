package main.commands.slashcommands.commands.util;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;

public class VoteCommand extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        ctx.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(ctx.getGuild(), RobertifyLocaleMessage.GeneralMessages.VOTE_EMBED_DESC)
                        .build())
                .setActionRow(
                        Button.of(ButtonStyle.LINK, "https://top.gg/bot/893558050504466482/vote", "Top.gg"),
                        Button.of(ButtonStyle.LINK, "https://discordbotlist.com/bots/robertify/upvote", "Discord Bot List")
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
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("vote")
                        .setDescription("Want to support us? Help spread our reach by voting for us!")
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Do you like Robertify and want to help us share it with more users? Do us the favour of voting for us! " +
                "It would really help us in growing our reach. 💖";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;

        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), RobertifyLocaleMessage.GeneralMessages.VOTE_EMBED_DESC)
                .build())
                .addActionRow(
                        Button.of(ButtonStyle.LINK, "https://top.gg/bot/893558050504466482/vote", "Top.gg"),
                        Button.of(ButtonStyle.LINK, "https://discordbotlist.com/bots/robertify/upvote", "Discord Bot List")
                )
                .queue();
    }
}
