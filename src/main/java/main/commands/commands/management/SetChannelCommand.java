package main.commands.commands.management;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.commands.commands.management.permissions.Permission;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.component.InteractiveCommand;
import main.utils.json.guildconfig.GuildConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
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
        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getAuthor(), Permission.ROBERTIFY_ADMIN))
            return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final Guild guild = ctx.getGuild();

        var guildConfig = new GuildConfig();

        if (args.isEmpty()) {
            TextChannel channel = ctx.getChannel();
            if (guildConfig.getAnnouncementChannelID(guild.getIdLong()) == channel.getIdLong()) {
                EmbedBuilder eb = EmbedUtils.embedMessage("This is already the announcement channel.");
                msg.replyEmbeds(eb.build()).queue();
            } else {
                guildConfig.setAnnouncementChannelID(guild.getIdLong(), channel.getIdLong());

                EmbedBuilder eb = EmbedUtils.embedMessage("Set the announcement channel to: " + channel.getAsMention());
                msg.replyEmbeds(eb.build()).queue();
            }
        } else {
            String id = GeneralUtils.getDigitsOnly(args.get(0));

            if (!GeneralUtils.stringIsID(id)) {
                EmbedBuilder eb = EmbedUtils.embedMessage("ID passed isn't a valid ID!");
                msg.replyEmbeds(eb.build()).queue();
                return;
            }

            TextChannel channel = Robertify.api.getGuildById(guild.getIdLong()).getTextChannelById(id);

            if (channel == null) {
                EmbedBuilder eb = EmbedUtils.embedMessage("That ID doesn't belong to any channel in this guild!");
                msg.replyEmbeds(eb.build()).queue();
                return;
            }

            guildConfig.setAnnouncementChannelID(guild.getIdLong(), channel.getIdLong());

            EmbedBuilder eb =  EmbedUtils.embedMessage("You've set the announcement channel to: " +  channel.getAsMention());
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

        var channel = event.getOption("channel").getAsGuildChannel();
        var guildConfig = new GuildConfig();

        if (!(channel instanceof TextChannel)) {
            event.replyEmbeds(EmbedUtils.embedMessage("The channel must be a **text** channel!").build())
                    .setEphemeral(true).queue();
            return;
        }

        if (guildConfig.getAnnouncementChannelID(event.getGuild().getIdLong()) == channel.getIdLong()) {
            event.replyEmbeds(EmbedUtils.embedMessage(channel.getAsMention() + " is already the announcement channel").build())
                    .setEphemeral(true).queue();
            return;
        }

        guildConfig.setAnnouncementChannelID(event.getGuild().getIdLong(), channel.getIdLong());
        event.replyEmbeds(EmbedUtils.embedMessage("You have set the announcement channel to: " + channel.getAsMention()).build())
                .setEphemeral(false)
                .queue();
    }
}
