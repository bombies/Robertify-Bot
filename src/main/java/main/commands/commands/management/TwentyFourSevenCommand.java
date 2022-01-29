package main.commands.commands.management;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.BotConstants;
import main.constants.Permission;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.InteractiveCommand;
import main.utils.json.guildconfig.GuildConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;

public class TwentyFourSevenCommand extends InteractiveCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        // TODO Add paywall logic

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
            return RobertifyEmbedUtils.embedMessage(guild, "You have turned 24/7 mode **on**").build();
        }
    }

    @Override
    public String getName() {
        return "24/7";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
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
                "Toggle whether the bot is supposed to stay in a voice channel 24/7 or not",
                    e -> GeneralUtils.hasPerms(e.getGuild(), e.getMember(), Permission.ROBERTIFY_ADMIN)
                ))
                .build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(getName())) return;

        // TODO Paywall Logic

        if (!getCommand().getCommand().permissionCheck(event)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(),
                    BotConstants.getInsufficientPermsMessage(Permission.ROBERTIFY_ADMIN)).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.replyEmbeds(logic(event.getGuild())).setEphemeral(false)
                .queue();
    }
}
