package main.commands.slashcommands.commands.management;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.BotConstants;
import main.constants.Permission;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.guildconfig.GuildConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;

public class TwentyFourSevenCommand extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var guild = ctx.getGuild();
        final var msg = ctx.getMessage();
        final var member = ctx.getMember();

        if (!GeneralUtils.hasPerms(guild, member, Permission.ROBERTIFY_ADMIN)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild,
                    BotConstants.getInsufficientPermsMessage(Permission.ROBERTIFY_ADMIN))
                    .build())
                    .queue();
            return;
        }

        msg.replyEmbeds(logic(guild)).queue();
    }

    private MessageEmbed logic(Guild guild) {
        final var config = new GuildConfig();

        if (config.get247(guild.getIdLong())) {
            config.set247(guild.getIdLong(), false);

            RobertifyAudioManager.getInstance().getMusicManager(guild)
                    .getScheduler().scheduleDisconnect(true);

            return RobertifyEmbedUtils.embedMessage(guild, "You have turned 24/7 mode **off**").build();
        } else {
            config.set247(guild.getIdLong(), true);

            RobertifyAudioManager.getInstance().getMusicManager(guild)
                    .getScheduler().removeScheduledDisconnect(guild);

            return RobertifyEmbedUtils.embedMessage(guild, "You have turned 24/7 mode **on**").build();
        }
    }

    @Override
    public String getName() {
        return "247";
    }

    @Override
    public String getHelp(String prefix) {
        return "Toggle whether or not the bot stays in a voice channel 24/7";
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("247")
                        .setDescription("Toggle whether the bot is supposed to stay in a voice channel 24/7 or not")
                        .setPossibleDJCommand()
                        .setPremium()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Toggle whether or not the bot stays in a voice channel 24/7";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checksWithPremium(event)) return;
        sendRandomMessage(event);

        event.replyEmbeds(logic(event.getGuild())).setEphemeral(false)
                .queue();
    }

    @Override
    public boolean isPremiumCommand() {
        return true;
    }
}
