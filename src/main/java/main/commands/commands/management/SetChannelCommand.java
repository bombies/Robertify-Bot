package main.commands.commands.management;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.Permission;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.legacy.InteractiveCommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.List;

public class SetChannelCommand extends InteractiveCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getMember(), Permission.ROBERTIFY_ADMIN))
            return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final Guild guild = ctx.getGuild();

        var guildConfig = new GuildConfig();

        if (args.isEmpty()) {
            TextChannel channel = ctx.getChannel();

            if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
                if (channel.getIdLong() == new DedicatedChannelConfig().getChannelID(guild.getIdLong())) {
                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "The announcement channel cannot be set to this channel!")
                            .build()).queue();
                    return;
                }

            if (guildConfig.getAnnouncementChannelID(guild.getIdLong()) == channel.getIdLong()) {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "This is already the announcement channel.");
                msg.replyEmbeds(eb.build()).queue();
            } else {
                guildConfig.setAnnouncementChannelID(guild.getIdLong(), channel.getIdLong());

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

            TextChannel channel = Robertify.api.getGuildById(guild.getIdLong()).getTextChannelById(id);

            if (channel == null) {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "That ID doesn't belong to any channel in this guild!");
                msg.replyEmbeds(eb.build()).queue();
                return;
            }

            if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
                if (channel.getIdLong() == new DedicatedChannelConfig().getChannelID(guild.getIdLong())) {
                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "The announcement channel cannot be set to that channel!")
                            .build()).queue();
                    return;
                }

            guildConfig.setAnnouncementChannelID(guild.getIdLong(), channel.getIdLong());

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
                        "Set the channel where all the now playing messages will be announced",
                        List.of(CommandOption.of(
                                OptionType.CHANNEL,
                                "channel",
                                "The channel to be set",
                                true
                        ))
                )).build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(getName())) return;

        final var channel = event.getOption("channel").getAsGuildChannel();
        final var guildConfig = new GuildConfig();
        final var guild = event.getGuild();

        if (!(channel instanceof TextChannel)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "The channel must be a **text** channel!").build())
                    .setEphemeral(true).queue();
            return;
        }

        if (guildConfig.getAnnouncementChannelID(event.getGuild().getIdLong()) == channel.getIdLong()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, channel.getAsMention() + " is already the announcement channel").build())
                    .setEphemeral(true).queue();
            return;
        }

        guildConfig.setAnnouncementChannelID(event.getGuild().getIdLong(), channel.getIdLong());
        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have set the announcement channel to: " + channel.getAsMention()).build())
                .setEphemeral(false)
                .queue();
    }
}
