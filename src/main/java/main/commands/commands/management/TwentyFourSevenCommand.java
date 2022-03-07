package main.commands.commands.management;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.BotConstants;
import main.constants.Permission;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.legacy.InteractiveCommand;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.votes.VoteManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;

public class TwentyFourSevenCommand extends InteractiveCommand implements ICommand {
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

    @Override
    public boolean isPremiumCommand() {
        return true;
    }
}
