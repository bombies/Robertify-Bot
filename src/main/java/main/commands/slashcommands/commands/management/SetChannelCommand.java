package main.commands.slashcommands.commands.management;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.Permission;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.requestchannel.RequestChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.List;

@Deprecated @ForRemoval
public class SetChannelCommand extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getMember(), Permission.ROBERTIFY_ADMIN))
            return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final Guild guild = ctx.getGuild();

        var guildConfig = new GuildConfig(guild);

        if (args.isEmpty()) {
            final var channel = ctx.getChannel();
            final var dedicatedChannelConfig = new RequestChannelConfig(guild);

            if (dedicatedChannelConfig.isChannelSet())
                if (channel.getIdLong() == dedicatedChannelConfig.getChannelID()) {
                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "The announcement channel cannot be set to this channel!")
                            .build()).queue();
                    return;
                }

            if (guildConfig.getAnnouncementChannelID() == channel.getIdLong()) {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "This is already the announcement channel.");
                msg.replyEmbeds(eb.build()).queue();
            } else {
                guildConfig.setAnnouncementChannelID(channel.getIdLong());

                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "Set the announcement channel to: " + channel.getAsMention());
                msg.replyEmbeds(eb.build()).queue();
            }
        } else {
            String id = GeneralUtils.getDigitsOnly(args.get(0));

            if (!GeneralUtils.stringIsID(id)) {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "ID provided isn't a valid ID!\n" +
                        "Make sure to either **mention** the channel, or provide its **ID**")
                        .setImage("https://i.imgur.com/Qg0BQ3f.png");
                msg.replyEmbeds(eb.build()).queue();
                return;
            }

            TextChannel channel = Robertify.shardManager.getGuildById(guild.getIdLong()).getTextChannelById(id);

            if (channel == null) {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "That ID doesn't belong to any channel in this guild!");
                msg.replyEmbeds(eb.build()).queue();
                return;
            }

            final var dedicatedChannelConfig = new RequestChannelConfig(guild);
            if (dedicatedChannelConfig.isChannelSet())
                if (channel.getIdLong() == dedicatedChannelConfig.getChannelID()) {
                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "The announcement channel cannot be set to that channel!")
                            .build()).queue();
                    return;
                }

            guildConfig.setAnnouncementChannelID(channel.getIdLong());

            EmbedBuilder eb =  RobertifyEmbedUtils.embedMessage(guild, "You've set the announcement channel to: " +  channel.getAsMention());
            msg.replyEmbeds(eb.build()).queue();
        }
    }

    @Override
    public String getName() {
        return "setchannel";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`" +
                "\nSet the announcement channel for when a new song is being played.\n\n" +
                "Usage: `"+ prefix +"setchannel <channelID>`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("sc");
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("setchannel")
                        .setDescription("Set the channel where all the now playing messages will be announced")
                        .addOptions(
                                CommandOption.of(
                                        OptionType.CHANNEL,
                                        "channel",
                                        "The channel to be set",
                                        false
                                )
                        )
                        .setAdminOnly()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;

        OptionMapping eventOption = event.getOption("channel");
        final var channel = eventOption != null ? eventOption.getAsChannel().asGuildMessageChannel() : event.getGuildChannel();
        final var guild = event.getGuild();
        final var guildConfig = new GuildConfig(guild);

        if (!(channel instanceof TextChannel)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "The channel must be a **text** channel!").build())
                    .setEphemeral(true).queue();
            return;
        }

        if (guildConfig.getAnnouncementChannelID() == channel.getIdLong()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, channel.getAsMention() + " is already the announcement channel").build())
                    .setEphemeral(true).queue();
            return;
        }

        guildConfig.setAnnouncementChannelID(channel.getIdLong());
        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have set the announcement channel to: " + channel.getAsMention()).build())
                .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()))
                .queue();
    }
}
