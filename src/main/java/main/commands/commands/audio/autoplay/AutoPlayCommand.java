package main.commands.commands.audio.autoplay;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.legacy.InteractiveCommand;
import main.utils.json.autoplay.AutoPlayConfig;
import main.utils.votes.VoteManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;

public class AutoPlayCommand extends InteractiveCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var msg = ctx.getMessage();

        msg.replyEmbeds(handleAutoPlay(ctx.getGuild())).queue();
    }

    public MessageEmbed handleAutoPlay(Guild guild) {
        AutoPlayConfig autoPlayConfig = new AutoPlayConfig();

        if (autoPlayConfig.getStatus(guild.getIdLong())) {
            autoPlayConfig.setStatus(guild.getIdLong(), false);
            return RobertifyEmbedUtils.embedMessage(guild, "You have toggled autoplay **OFF**!").build();
        } else {
            autoPlayConfig.setStatus(guild.getIdLong(), true);
            return RobertifyEmbedUtils.embedMessage(guild, "You have toggled autoplay **ON**!").build();
        }
    }

    @Override
    public String getName() {
        return "autoplay";
    }

    @Override
    public String getHelp(String prefix) {
        return "Autoplay allows Robertify to keep playing music just like the last one at the end of the queue." +
                " Running this command will toggle autoplay either on or off.";
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
                        "Play recommended tracks at the end of your queue!",
                        djPredicate
                ))
                .build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(getName())) return;

        if (isPremiumCommand()) {
            if (!new VoteManager().userVoted(event.getUser().getId(), VoteManager.Website.TOP_GG)) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(event.getGuild(),
                                "🔒 Locked Command", """
                                                    Woah there! You must vote before interacting with this command.
                                                    Click on each of the buttons below to vote!

                                                    *Note: Only the first two votes sites are required, the last two are optional!*""").build())
                        .addActionRow(
                                Button.of(ButtonStyle.LINK, "https://top.gg/bot/893558050504466482/vote", "Top.gg"),
                                Button.of(ButtonStyle.LINK, "https://discordbotlist.com/bots/robertify/upvote", "Discord Bot List"),
                                Button.of(ButtonStyle.LINK, "https://discords.com/bots/bot/893558050504466482/vote", "Discords.com"),
                                Button.of(ButtonStyle.LINK, "https://discord.boats/bot/893558050504466482/vote", "Discord.boats")
                        )
                        .setEphemeral(true)
                        .queue();
                return;
            }
        }

        Guild guild = event.getGuild();
        if (!getCommand().getCommand().permissionCheck(event)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be a DJ to run this command!").build())
                    .queue();
            return;
        }

        event.replyEmbeds(handleAutoPlay(guild)).queue();
    }

    @Override
    public boolean isPremiumCommand() {
        return true;
    }
}
