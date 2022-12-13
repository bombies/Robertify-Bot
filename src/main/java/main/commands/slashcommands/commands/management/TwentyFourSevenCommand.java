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
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.internal.utils.tuple.Pair;
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
                    BotConstants.getInsufficientPermsMessage(guild, Permission.ROBERTIFY_ADMIN))
                    .build())
                    .queue();
            return;
        }

        msg.replyEmbeds(logic(guild)).queue();
    }

    private MessageEmbed logic(Guild guild) {
        final var config = new GuildConfig(guild);
        final var localeManager = LocaleManager.getLocaleManager(guild);

        if (config.get247()) {
            config.set247(false);

            RobertifyAudioManager.getInstance().getMusicManager(guild)
                    .getScheduler().scheduleDisconnect(true);

            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TwentyFourSevenMessages.TWENTY_FOUR_SEVEN_TOGGLED,
                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
            ).build();
        } else {
            config.set247(true);

            RobertifyAudioManager.getInstance().getMusicManager(guild)
                    .getScheduler().
                    removeScheduledDisconnect();

            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TwentyFourSevenMessages.TWENTY_FOUR_SEVEN_TOGGLED,
                    Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
            ).build();
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

        event.replyEmbeds(logic(event.getGuild())).setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()))
                .queue();
    }

    @Override
    public boolean isPremiumCommand() {
        return true;
    }
}
