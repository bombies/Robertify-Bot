package main.commands.slashcommands.commands.dev;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.IDevCommand;
import main.commands.RandomMessageManager;
import main.constants.ENV;
import main.main.Config;
import main.main.Robertify;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.locale.LocaleManager;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;

public class ReloadConfigCommand extends AbstractSlashCommand implements IDevCommand {
    private final static Logger logger = LoggerFactory.getLogger(ReloadConfigCommand.class);

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        try {
            Config.reload();
            LocaleManager.reloadLocales();
            Robertify.initVoteSiteAPIs();
            RandomMessageManager.setChance(Double.parseDouble(Config.get(ENV.RANDOM_MESSAGE_CHANCE)));

            ctx.getMessage().addReaction("✅").queue();
        } catch (Exception e) {
            logger.error("There was an unexpected error!", e);
            ctx.getMessage().addReaction("❌").queue();
        }
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("reload")
                        .setDescription("Reload all config files into memory")
                        .setDevCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!devCheck(event))
            return;

        try {
            Config.reload();
            RandomMessageManager.setChance(Double.parseDouble(Config.get(ENV.RANDOM_MESSAGE_CHANCE)));
            event.reply("Successfully reloaded all configs!").setEphemeral(true).queue();
        } catch (Exception e) {
            logger.error("There was an unexpected error!", e);
            event.reply("There was an unexpected error!\n```" + e.getMessage() + "```")
                    .setEphemeral(true)
                    .queue();
        }
    }
}
