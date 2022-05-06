package main.commands.slashcommands.commands.management;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.BotConstants;
import main.constants.Permission;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.restrictedchannels.RestrictedChannelsConfig;
import main.constants.Toggles;
import main.utils.GeneralUtils;
import main.utils.json.toggles.TogglesConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.List;

public class RestrictedChannelsCommand extends AbstractSlashCommand implements ICommand {
    private final static Logger logger = LoggerFactory.getLogger(RestrictedChannelsCommand.class);

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Guild guild = ctx.getGuild();

        if (!GeneralUtils.hasPerms(guild, ctx.getMember(), Permission.ROBERTIFY_ADMIN)) return;

        final Message msg = ctx.getMessage();
        final List<String> args = ctx.getArgs();
        final String prefix = new GuildConfig().getPrefix(guild.getIdLong());

        if (args.isEmpty()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Insufficient arguments!\n\n" + getUsages(prefix)).build())
                    .queue();
        } else {
            switch (args.get(0).toLowerCase()) {
                case "add" -> add(msg, args);
                case "remove" -> remove(msg, args);
                case "list" -> list(msg);
                default -> msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Invalid arguments!\n\n" + getUsages(prefix)).build())
                        .queue();
            }
        }
    }

    public void add(Message msg, List<String> args) {
        final var guild = msg.getGuild();

        if (args.size() < 2) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, """
                    You must provide the voice channel you want to restrict
                    **TIP**: You must enable *developer mode* to view the ID of the voice or text channel. (https://bit.ly/32wGtRz)

                    **__Example__**
                    `restrictedchannels add 842795162513965066`
                    `restrictedchannels add #bot-commands`
                    """).build()).queue();
            return;
        }

        final String id = args.get(1);

        if (!GeneralUtils.stringIsID(id)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid voice channel ID!\n" +
                            "Make sure to either **mention** the channel, or provide its **ID**")

                            .build())
                    .queue();
            return;
        }

        final VoiceChannel voiceChannel = guild.getVoiceChannelById(GeneralUtils.getDigitsOnly(id));
        final TextChannel textChannel = guild.getTextChannelById(GeneralUtils.getDigitsOnly(id));
        final long channelId;
        final RestrictedChannelsConfig.ChannelType field;

        if (voiceChannel != null) {
            if (!new TogglesConfig().getToggle(guild, Toggles.RESTRICTED_VOICE_CHANNELS)) {
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, """
                                    This feature is toggled **OFF**.

                                    *Looking to toggle this feature on? Do* `toggle restrictedvoice`""")
                                .build())
                        .queue();
                return;
            }

            channelId = voiceChannel.getIdLong();
            field = RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL;
        } else if (textChannel != null) {
            if (!new TogglesConfig().getToggle(guild, Toggles.RESTRICTED_TEXT_CHANNELS)) {
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, """
                                    This feature is toggled **OFF**.

                                    *Looking to toggle this feature on? Do* `toggle restrictedtext`""")
                                .build())
                        .queue();
                return;
            }

            channelId = textChannel.getIdLong();
            field = RestrictedChannelsConfig.ChannelType.TEXT_CHANNEL;
        } else {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "The ID provided was not of a valid voice or text channel" +
                            " in this server.").build())
                    .queue();
            return;
        }

        try {
            new RestrictedChannelsConfig().addChannel(guild.getIdLong(), channelId, field);
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have successfully added <#"
                            + channelId + "> as a restricted "+ field +" channel!").build())
                    .queue();
        } catch (IllegalStateException e) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, e.getMessage()).build())
                    .queue();
        } catch (Exception e) {
            logger.error("Unexpected error!", e);
            msg.addReaction("❌").queue();
        }
    }

    public void remove(Message msg, List<String> args) {
        final var guild = msg.getGuild();

        if (args.size() < 2) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, """
                    You must provide the voice channel you want to remove from the restriction list
                    **TIP**: You must enable *developer mode* to view the ID of the voice or text channel. (https://bit.ly/32wGtRz)

                    **__Example__**
                    `restrictedchannels remove 842795162513965066`
                    `restrictedchannels remove #bot-commands`
                    """).build()).queue();
            return;
        }

        final String id = args.get(1);

        if (!GeneralUtils.stringIsID(id)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid voice channel ID!").build())
                    .queue();
            return;
        }

        final VoiceChannel voiceChannel = guild.getVoiceChannelById(GeneralUtils.getDigitsOnly(id));
        final TextChannel textChannel = guild.getTextChannelById(GeneralUtils.getDigitsOnly(id));
        final long channelId;
        final RestrictedChannelsConfig.ChannelType field;

        if (voiceChannel != null) {
            if (!new TogglesConfig().getToggle(guild, Toggles.RESTRICTED_VOICE_CHANNELS)) {
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, """
                                    This feature is toggled **OFF**.

                                    *Looking to toggle this feature on? Do* `toggle restrictedvoice`""")
                                .build())
                        .queue();
                return;
            }

            channelId = voiceChannel.getIdLong();
            field = RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL;
        } else if (textChannel != null) {
            if (!new TogglesConfig().getToggle(guild, Toggles.RESTRICTED_TEXT_CHANNELS)) {
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, """
                                    This feature is toggled **OFF**.

                                    *Looking to toggle this feature on? Do* `toggle restrictedtext`""")
                                .build())
                        .queue();
                return;
            }

            channelId = textChannel.getIdLong();
            field = RestrictedChannelsConfig.ChannelType.TEXT_CHANNEL;
        } else {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "The ID provided was not of a valid voice or text channel" +
                            " in this server.").build())
                    .queue();
            return;
        }

        try {
            new RestrictedChannelsConfig().removeChannel(guild.getIdLong(), channelId, field);
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have successfully removed <#"
                            + channelId + "> as a restricted "+ field +" channel!").build())
                    .queue();
        } catch (IllegalStateException | NullPointerException e) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, e.getMessage()).build())
                    .queue();
        } catch (Exception e) {
            logger.error("Unexpected error!", e);
            msg.addReaction("❌").queue();
        }
    }

    public void list(Message msg) {
        final var config = new RestrictedChannelsConfig();
        final var guild = msg.getGuild();

        try {
            final var tcs = config.restrictedChannelsToString(guild.getIdLong(), RestrictedChannelsConfig.ChannelType.TEXT_CHANNEL);
            final var vcs = config.restrictedChannelsToString(guild.getIdLong(), RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL);

            EmbedBuilder embedBuilder = RobertifyEmbedUtils.embedMessage(guild, "Listing all restricted channels");
            embedBuilder.addField("Text Channels", tcs == null ? "No channels" : tcs.isEmpty() ? "No channels" : tcs, false);
            embedBuilder.addField("Voice Channels", vcs == null ? "No channels" : vcs.isEmpty() ? "No channels" : vcs, false);

            msg.replyEmbeds(embedBuilder.build()).queue();
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            msg.addReaction("❌").queue();
        }
    }

    @Override
    public String getName() {
        return "restrictedchannels";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`" +
                "\nRestrict the bot to join voice/text channels that you set.\n\n"
                + getUsages(prefix);
    }

    @Override
    public String getUsages(String prefix) {
        return "**__Usages__**\n" +
                "`"+ prefix +"restrictedchannels add <channelID>`\n" +
                "`"+ prefix +"restrictedchannels remove <channelID>`\n" +
                "`"+ prefix +"restrictedchannels list`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("rc", "rchannels");
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("restrictedchannels")
                        .setDescription("Configure which channels Robertify can interact with!")
                        .addSubCommands(
                                SubCommand.of(
                                        "add",
                                        "Add restricted text or voice channels",
                                        List.of(
                                                CommandOption.of(
                                                        OptionType.CHANNEL,
                                                        "channel",
                                                        "The channel to add a restricted channel",
                                                        true
                                                )
                                        )
                                ),
                                SubCommand.of(
                                        "remove",
                                        "Remove restricted text or voice channels",
                                        List.of(
                                                CommandOption.of(
                                                        OptionType.CHANNEL,
                                                        "channel",
                                                        "The channel to add a restricted channel",
                                                        true
                                                )
                                        )
                                ),
                                SubCommand.of(
                                        "list",
                                        "List all restricted voice channels"
                                )
                        )
                        .setAdminOnly()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Restrict the bot to join voice/text channels that you set.";
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checks(event)) return;

        final var config = new RestrictedChannelsConfig();
        switch (event.getSubcommandName()) {
            case "add" -> {
                final var channel = event.getOption("channel").getAsGuildChannel();
                final var guild = event.getGuild();
                final long channelId;
                final RestrictedChannelsConfig.ChannelType field;

                if (channel.getType().isAudio()) {
                    if (!new TogglesConfig().getToggle(guild, Toggles.RESTRICTED_VOICE_CHANNELS)) {
                        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, """
                                    This feature is toggled **OFF**.

                                    *Looking to toggle this feature on? Do* `toggle restrictedvoice`""")
                                        .build())
                                .queue();
                        return;
                    }

                    channelId = channel.getIdLong();
                    field = RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL;
                } else if (channel.getType().isMessage()) {
                    if (!new TogglesConfig().getToggle(guild, Toggles.RESTRICTED_TEXT_CHANNELS)) {
                        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, """
                                    This feature is toggled **OFF**.

                                    *Looking to toggle this feature on? Do* `toggle restrictedtext`""")
                                        .build())
                                .queue();
                        return;
                    }

                    channelId = channel.getIdLong();
                    field = RestrictedChannelsConfig.ChannelType.TEXT_CHANNEL;
                } else {
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "The ID provided was not of a valid voice or text channel" +
                                    " in this server.").build())
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                try {
                    config.addChannel(guild.getIdLong(), channelId, field);
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have successfully added <#"
                                    + channelId + "> as a restricted "+ field +" channel!").build())
                            .queue();
                } catch (IllegalStateException e) {
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, e.getMessage()).build())
                            .setEphemeral(true)
                            .queue();
                } catch (Exception e) {
                    logger.error("Unexpected error!", e);
                    event.replyEmbeds(BotConstants.getUnexpectedErrorEmbed(guild))
                            .setEphemeral(true)
                            .queue();
                }
            }
            case "remove" -> {
                final var channel = event.getOption("channel").getAsGuildChannel();
                final var guild = event.getGuild();
                final long channelId;
                final RestrictedChannelsConfig.ChannelType field;

                if (channel.getType().isAudio()) {
                    if (!new TogglesConfig().getToggle(guild, Toggles.RESTRICTED_VOICE_CHANNELS)) {
                        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, """
                                    This feature is toggled **OFF**.

                                    *Looking to toggle this feature on? Do* `toggle restrictedvoice`""")
                                        .build())
                                .queue();
                        return;
                    }

                    channelId = channel.getIdLong();
                    field = RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL;
                } else if (channel.getType().isMessage()) {
                    if (!new TogglesConfig().getToggle(guild, Toggles.RESTRICTED_TEXT_CHANNELS)) {
                        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, """
                                    This feature is toggled **OFF**.

                                    *Looking to toggle this feature on? Do* `toggle restrictedtext`""")
                                        .build())
                                .queue();
                        return;
                    }

                    channelId = channel.getIdLong();
                    field = RestrictedChannelsConfig.ChannelType.TEXT_CHANNEL;
                } else {
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "The ID provided was not of a valid voice or text channel" +
                                    " in this server.").build())
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                try {
                    config.removeChannel(guild.getIdLong(), channelId, field);
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have successfully removed <#"
                                    + channelId + "> as a restricted "+ field +" channel!").build())
                            .queue();
                } catch (IllegalStateException | NullPointerException e) {
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, e.getMessage()).build())
                            .setEphemeral(true)
                            .queue();
                } catch (Exception e) {
                    logger.error("Unexpected error!", e);
                    event.replyEmbeds(BotConstants.getUnexpectedErrorEmbed(guild))
                            .setEphemeral(true)
                            .queue();
                }
            }
            case "list" -> {
                final var guild = event.getGuild();

                try {
                    final var tcs = config.restrictedChannelsToString(guild.getIdLong(), RestrictedChannelsConfig.ChannelType.TEXT_CHANNEL);
                    final var vcs = config.restrictedChannelsToString(guild.getIdLong(), RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL);

                    EmbedBuilder embedBuilder = RobertifyEmbedUtils.embedMessage(guild, "Listing all restricted channels");
                    embedBuilder.addField("Text Channels", tcs == null ? "No channels" : tcs.isEmpty() ? "No channels" : tcs, false);
                    embedBuilder.addField("Voice Channels", vcs == null ? "No channels" : vcs.isEmpty() ? "No channels" : vcs, false);

                    event.replyEmbeds(embedBuilder.build()).queue();
                } catch (Exception e) {
                    logger.error("Unexpected error", e);
                    event.replyEmbeds(BotConstants.getUnexpectedErrorEmbed(guild))
                            .setEphemeral(true)
                            .queue();
                }
            }
        }
    }
}
