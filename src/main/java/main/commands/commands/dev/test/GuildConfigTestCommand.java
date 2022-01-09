package main.commands.commands.dev.test;

import main.commands.CommandContext;
import main.commands.ITestCommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.guildconfig.GuildConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;

import javax.script.ScriptException;
import java.util.List;

public class GuildConfigTestCommand implements ITestCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        final var args = ctx.getArgs();
        final var config = new GuildConfig();
        final var guild = ctx.getGuild();
        final var msg = ctx.getMessage();

        if (args.isEmpty()) {
            final var guildHasInfo = config.guildHasInfo(guild.getIdLong());
            final var prefix = config.getPrefix(guild.getIdLong());
            final var announcementChannel = config.getAnnouncementChannelID(guild.getIdLong());
            final var bannedUsers = config.getBannedUsers(guild.getIdLong());

            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "\t")
                    .addField("Has Info", String.valueOf(guildHasInfo), false)
                    .addField("Prefix", prefix, false)
                    .addField("Announcement Channel", String.valueOf(announcementChannel), false)
                    .addField("Banned Users", bannedUsers.toString(), false)
                    .build()
            ).queue();
        } else {
            if (args.get(0).equalsIgnoreCase("set")) {
                if (args.size() < 3) {
                    msg.reply("Add more args lol").queue();
                    return;
                }

                long startTime = System.currentTimeMillis();
                try {
                    switch (args.get(1).toLowerCase()) {
                        case "prefix" -> {
                            config.setPrefix(guild.getIdLong(), args.get(2));
                        }
                        case "ac" -> {
                            config.setAnnouncementChannelID(guild.getIdLong(), Long.parseLong(args.get(2)));
                        }
                        case "banuser" -> {
                            config.banUser(guild.getIdLong(), Long.parseLong(args.get(2)), 0, 0, 0);
                        }
                        case "unbanuser" -> {
                            config.unbanUser(guild.getIdLong(), Long.parseLong(args.get(2)));
                        }
                        default -> {
                            return;
                        }
                    }
                    msg.addReaction("✅").queue(success -> msg.reply("Took " + (System.currentTimeMillis() - startTime) + " ms").queue());
                } catch (Exception e) {
                    msg.reply(e.getMessage()).queue();
                }
            }
        }

    }

    @Override
    public String getName() {
        return "guildconfigtest";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return List.of("gct");
    }
}
