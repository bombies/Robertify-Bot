package main.commands.slashcommands.commands.management;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.BotConstants;
import main.constants.Permission;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.logs.LogConfig;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.List;

public class SetLogChannelCommand extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Guild guild = ctx.getGuild();
        final Message message = ctx.getMessage();

        if (!GeneralUtils.hasPerms(guild, ctx.getMember(), Permission.ROBERTIFY_ADMIN)) {
            message.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, BotConstants.getInsufficientPermsMessage(guild, Permission.ROBERTIFY_ADMIN)).build())
                    .queue();
            return;
        }

        final var args = ctx.getArgs();

        if (args.isEmpty()) {
            message.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LogChannelMessages.LOG_CHANNEL_MISSING).build())
                    .queue();
            return;
        }

        final var channelID = args.get(0);

        if (!GeneralUtils.stringIsID(channelID)) {
            message.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LogChannelMessages.INVALID_LOG_CHANNEL).build())
                    .queue();
            return;
        }

        final var channel = guild.getTextChannelById(channelID);

        if (channel == null) {
            message.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NO_SUCH_CHANNEL).build())
                    .queue();
            return;
        }

        final var dedicatedChannelConfig = new DedicatedChannelConfig(guild);
        if (dedicatedChannelConfig.isChannelSet()) {
            if (dedicatedChannelConfig.getChannelID() == channel.getIdLong()) {
                message.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LogChannelMessages.CANNOT_SET_LOG_CHANNEL).build())
                        .queue();
                return;
            }
        }

        final var config = new LogConfig(guild);

        config.setChannel(channel.getIdLong());
        message.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LogChannelMessages.LOG_CHANNEL_SET, Pair.of("{channel}", channel.getAsMention())).build())
                .queue();
    }

    @Override
    public String getName() {
        return "setlogchannel";
    }

    @Override
    public List<String> getAliases() {
        return List.of("slc");
    }

    @Override
    public String getHelp(String prefix) {
        return "Run this command to set where logs should be sent!";
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("setlogchannel")
                        .setDescription("Run this command to set where logs should be sent!")
                        .addOptions(
                                CommandOption.of(
                                        OptionType.CHANNEL,
                                        "channel",
                                        "The channel to set as the new log channel",
                                        true
                                )
                        )
                        .setAdminOnly()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Run this command to set where logs should be sent!";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;

        final Guild guild = event.getGuild();

        final var channel = event.getOption("channel").getAsChannel().asGuildMessageChannel();

        if (!channel.getType().isMessage()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LogChannelMessages.LOG_CHANNEL_INVALID_TYPE).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final var dedicatedChannelConfig = new DedicatedChannelConfig(guild);
        if (dedicatedChannelConfig.isChannelSet()) {
            if (dedicatedChannelConfig.getChannelID() == channel.getIdLong()) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LogChannelMessages.CANNOT_SET_LOG_CHANNEL).build())
                        .setEphemeral(true)
                        .queue();
                return;
            }
        }

        final var config = new LogConfig(guild);

        if (config.channelIsSet()) {
            TextChannel oldChannel = config.getChannel();
            config.removeChannel();
            oldChannel.delete().queue();
        }

        config.setChannel(channel.getIdLong());
        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LogChannelMessages.LOG_CHANNEL_SET, Pair.of("{channel}", channel.getAsMention())).build())
                .queue();
    }
}
