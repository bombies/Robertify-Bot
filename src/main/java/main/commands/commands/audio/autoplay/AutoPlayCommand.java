package main.commands.commands.audio.autoplay;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
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

public class AutoPlayCommand extends AbstractSlashCommand implements ICommand {
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
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("autoplay")
                        .setDescription("Play recommended tracks at the end of your queue!")
                        .setDJOnly()
                        .setPremium()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Autoplay allows Robertify to keep playing music just like the last one at the end of the queue." +
                " Running this command will toggle autoplay either on or off.";
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!nameCheck(event)) return;
        if (!premiumCheck(event)) return;

        Guild guild = event.getGuild();
        if (!musicCommandDJCheck(event)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be a DJ to run this command!").build())
                    .setEphemeral(true)
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
