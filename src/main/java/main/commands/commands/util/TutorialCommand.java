package main.commands.commands.util;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.GeneralUtils;
import main.utils.database.ServerDB;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TutorialCommand implements ICommand {
    private static List<User> usersInTutorial = new ArrayList<>();

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Guild guild = ctx.getGuild();
        final User user = ctx.getAuthor();
        final Message msg = ctx.getMessage();
        final String prefix = ServerDB.getPrefix(guild.getIdLong());

        GeneralUtils.setCustomEmbed("Tutorial");

        if (userInTutorial(user)) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You are already in the tutorial!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        usersInTutorial.add(user);

        EmbedBuilder welcomeEmbed = EmbedUtils.embedMessage("\t");
        welcomeEmbed.addField(
                "Welcome",
                "Welcome to Robertify; a Discord music bot with an easily understood interface and is easy to use!",
                false
        );
        welcomeEmbed.addField(
                "Features",
                """
                        - Smooth playback
                        - Server-specific prefixes
                        - Server-specific permissions
                        - Clean and beautiful embedded menus
                        - Supports streaming services such as YouTube and Spotify.
                        - Playlist support (Both YouTube and Spotify)
                                                
                                                
                        \s""" + getNextMessage(10) + "",
                false
        );
        msg.replyEmbeds(welcomeEmbed.build()).queue(welcomeMsg -> {
            EmbedBuilder playingSongFromYoutube = EmbedUtils.embedMessage("\t");
            playingSongFromYoutube.addField(
                    "Playing Songs From YouTube",
                    """
                            Playing songs from YouTube is actually a really simple process.
                            All you have to do is type 
                            `""" +prefix+
                            """
                            play <song_name OR  youtube_link> `
                            """,
                    false
            );
            playingSongFromYoutube.setImage("https://i.imgur.com/27lnaYZ.gif");
            playingSongFromYoutube.setFooter(getNextMessage(10));
            msg.replyEmbeds(playingSongFromYoutube.build()).queueAfter(10, TimeUnit.SECONDS, youTubeMessage -> {
                EmbedBuilder playingSongFromSpotify = EmbedUtils.embedMessage("\t");
                playingSongFromSpotify.addField(
                        "Playing Songs From Spotify",
                        """
                                Playing songs from Spotify is also a simple process.
                                All you have to do is type 
                                `""" +prefix+
                                """
                                play <playlist_link OR song_link OR album_link> `
                                """,
                        false
                );
                playingSongFromSpotify.setImage("https://i.imgur.com/vxhvN79.gif");
                playingSongFromSpotify.setFooter(getNextMessage(10));
                msg.replyEmbeds(playingSongFromSpotify.build()).queueAfter(10, TimeUnit.SECONDS, spotifyMessage -> {
                    EmbedBuilder queueManipulation = EmbedUtils.embedMessage("\t");
                    queueManipulation.addField(
                            "What is the queue?",
                            "The queue is the sequence of songs awaiting to be played by the bot.",
                            false
                    );
                    queueManipulation.addField(
                            "How can I access the queue?",
                            "You can access the queue by running the command `"+prefix+"queue`\n" +
                                    "This brings up a menu where you can traverse through the queue pages (If applicable)",
                            false
                    );
                    queueManipulation.addField(
                            "How do I add songs to the queue",
                            "Upon running the `play` command, the song that is to be played will automatically be added to the queue",
                            false
                    );
                    queueManipulation.addField(
                            "How do I remove songs from the queue",
                            "In the queue, each song is assigned an **identification number**. This can be identified by" +
                                    " running the `queue` command and seeing the number beside the song's name\n\n" +
                                    "You can use this ID number to remove songs from the queue by running `"+prefix+"remove <id>`",
                            false
                    );
                    queueManipulation.addField(
                            "How do I move songs in the queue",
                            "As was stated earlier, each song is assigned an **identification number**." +
                                    "You can use this ID number to move songs from the queue by running `"+prefix+"move <id> <position>`\n" +
                                    "For example: If you want to move a song with ID #10 to position 2, you'd have to run `"+prefix+"move 10 2`",
                            false
                    );
                    queueManipulation.setImage("https://i.imgur.com/1PWkKef.gif");
                    queueManipulation.setFooter(getNextMessage(20));
                    msg.replyEmbeds(queueManipulation.build()).queueAfter(10, TimeUnit.SECONDS, queueManipMsg -> {
                        EmbedBuilder playerManipulation = EmbedUtils.embedMessage("\t");
                        playerManipulation.addField(
                                "How do I skip a song?",
                                "You can easily skip a song by running `"+prefix+"skip`",
                                false
                        );
                        playerManipulation.addField(
                                "How do I skip to a specific song in the queue",
                                "You can skip to a specific song in the queue very easily. You must identify the" +
                                        " ID number of the song in the queue by running the `queue` command. Once you've done that " +
                                        "you can run `"+prefix+"skipto <id>` to skip to that specific song",
                                false
                        );
                        playerManipulation.addField(
                                "How can I rewind a song?",
                                "You can rewind the song by running the command `"+prefix+"rewind <seconds_to_repeat>`",
                                false
                        );
                        playerManipulation.addField(
                                "How can I fast-forward a song?",
                                "You can fast-forward the song by running the command `"+prefix+"jump <seconds_to_jump>`",
                                false
                        );
                        playerManipulation.addField(
                                "How can I see the current song being played?",
                                "You can see the song being currently played by running the `"+prefix+"nowplaying` command.",
                                false
                        );
                        playerManipulation.setImage("https://i.imgur.com/N1FrmXW.gif");
                        msg.replyEmbeds(playerManipulation.build()).queueAfter(20, TimeUnit.SECONDS, playerManipulationMsg -> {
                            EmbedBuilder endOfTutorial = EmbedUtils.embedMessage("That's the end of the tutorial!\n" +
                                    "Need more help? Trying running the `help` command!\n" +
                                    "Thanks for using Robertify! 💖\n" +
                                    "\n*- Robertify Dev Team*");
                            usersInTutorial.remove(user);
                            msg.replyEmbeds(endOfTutorial.build()).queueAfter(20, TimeUnit.SECONDS, eot -> usersInTutorial.remove(user));
                        });
                    });
                });
            });
        });

        GeneralUtils.setDefaultEmbed();
    }

    @Override
    public String getName() {
        return "tutorial";
    }

    @Override
    public String getHelp(String guildID) {
        return null;
    }

    private String getNextMessage(int seconds) {
        return "The next message will come up in " + seconds + " seconds...";
    }

    public static boolean userInTutorial(User user) {
        return usersInTutorial.contains(user);
    }
}
