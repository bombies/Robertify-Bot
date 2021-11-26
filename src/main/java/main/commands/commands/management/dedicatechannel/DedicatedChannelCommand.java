package main.commands.commands.management.dedicatechannel;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.commands.commands.management.permissions.Permission;
import main.constants.BotConstants;
import main.constants.ENV;
import main.main.Config;
import main.utils.GeneralUtils;
import main.utils.database.ServerUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.managers.ChannelManager;

import javax.script.ScriptException;

public class DedicatedChannelCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getAuthor(), Permission.ROBERTIFY_ADMIN)) {
            ctx.getMessage().replyEmbeds(EmbedUtils.embedMessage("You do not have permission to execute this command")
                    .build()).queue();
            return;
        }

        final Message msg = ctx.getMessage();
        final Guild guild = ctx.getGuild();

        guild.createTextChannel("robertify-requests").queue(
                textChannel -> {
                    var dediChannelConfig = new DedicatedChannelConfig();

                    ChannelManager manager = textChannel.getManager();
                    manager.setPosition(0).queue();
                    manager.setTopic(
                                    BotConstants.REWIND_EMOJI + " Rewind the song. " +
                                    BotConstants.PLAY_AND_PAUSE_EMOJI + " Pause/Resume the song. " +
                                    BotConstants.END_EMOJI + " Skip the song. " +
                                    BotConstants.LOOP_EMOJI + " Loop the song. " +
                                    BotConstants.SHUFFLE_EMOJI + " Shuffle the song. "

                    ).queue();

                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setColor(GeneralUtils.parseColor(Config.get(ENV.BOT_COLOR)));
                    eb.setTitle("No song playing...");
                    eb.setImage("https://www.pixelstalk.net/wp-content/uploads/images5/Neon-Green-Aesthetic-HD-Wallpaper-Free-download.jpg");
                    eb.setFooter("Prefix for this server is: " + ServerUtils.getPrefix(guild.getIdLong()));


                    textChannel.sendMessage("**__Queue:__**\nJoin a voice channel and start playing songs!").setEmbeds(eb.build())
                            .queue(message -> {
                                dediChannelConfig.setChannelAndMessage(guild.getId(), textChannel.getId(), message.getId());

                                message.editMessageComponents(ActionRow.of(
                                        Button.of(ButtonStyle.SECONDARY, ButtonID.REWIND.toString(), Emoji.fromMarkdown(BotConstants.REWIND_EMOJI.toString())),
                                        Button.of(ButtonStyle.SECONDARY, ButtonID.PLAY_AND_PAUSE.toString(), Emoji.fromMarkdown(BotConstants.PLAY_AND_PAUSE_EMOJI.toString())),
                                        Button.of(ButtonStyle.SECONDARY, ButtonID.END.toString(), Emoji.fromMarkdown(BotConstants.END_EMOJI.toString())),
                                        Button.of(ButtonStyle.SECONDARY, ButtonID.LOOP.toString(), Emoji.fromMarkdown(BotConstants.LOOP_EMOJI.toString())),
                                        Button.of(ButtonStyle.SECONDARY, ButtonID.SHUFFLE.toString(), Emoji.fromMarkdown(BotConstants.SHUFFLE_EMOJI.toString()))
                                )).queue();
                            });

                    msg.addReaction("✅").queue();
                }
        );

    }

    @Override
    public String getName() {
        return "setup";
    }

    @Override
    public String getHelp(String guildID) {
        return "";
    }

    public enum ButtonID {
        REWIND("dedicatedrewind"),
        PLAY_AND_PAUSE("dedicatedpnp"),
        END("dedicatedend"),
        LOOP("dedicatedloop"),
        SHUFFLE("dedicatedshuffle");

        private final String str;

        ButtonID(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
