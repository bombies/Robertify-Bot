package main.commands.prefixcommands.dev.test;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ITestCommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.guildconfig.GuildConfig;

import javax.script.ScriptException;
import java.util.List;

public class GuildConfigTestCommand implements ITestCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        final var args = ctx.getArgs();
        final var guild = ctx.getGuild();
        final var config = new GuildConfig(guild);
        final var msg = ctx.getMessage();

        if (args.isEmpty()) {
            final var guildHasInfo = config.guildHasInfo();
            final var prefix = config.getPrefix();
            final var announcementChannel = config.getAnnouncementChannelID();
            final var bannedUsers = config.getBannedUsers();

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
                            config.setPrefix(args.get(2));
                        }
                        case "ac" -> {
                            config.setAnnouncementChannelID(Long.parseLong(args.get(2)));
                        }
                        case "banuser" -> {
                            config.banUser(Long.parseLong(args.get(2)), 0, 0, 0);
                        }
                        case "unbanuser" -> {
                            config.unbanUser(Long.parseLong(args.get(2)));
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
