package main.commands.slashcommands.commands.audio;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.autoplay.AutoPlayConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;

public class AutoPlayCommand extends AbstractSlashCommand implements ICommand {

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var msg = ctx.getMessage();

        msg.replyEmbeds(handleAutoPlay(ctx.getGuild())).queue();
    }

    public MessageEmbed handleAutoPlay(Guild guild) {
        final var localeManager = LocaleManager.getLocaleManager(guild);

        final var autoPlayConfig = new AutoPlayConfig(guild);

        if (autoPlayConfig.getStatus()) {
            autoPlayConfig.setStatus(false);
            return RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.GENERAL_TOGGLE_MESSAGE, Pair.of("{toggle}", "autoplay"), Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase()))).build();
        } else {
            autoPlayConfig.setStatus(true);
            return RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.GENERAL_TOGGLE_MESSAGE, Pair.of("{toggle}", "autoplay"), Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase()))).build();
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
        if (!checksWithPremium(event)) return;
        sendRandomMessage(event);

        Guild guild = event.getGuild();

        event.replyEmbeds(handleAutoPlay(guild)).queue();
    }

    @Override
    public boolean isPremiumCommand() {
        return true;
    }
}
