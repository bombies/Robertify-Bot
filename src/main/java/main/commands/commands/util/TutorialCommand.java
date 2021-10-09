package main.commands.commands.util;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.GeneralUtils;
import main.utils.database.ServerUtils;
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
        final String prefix = ServerUtils.getPrefix(guild.getIdLong());

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
                                                
                                                
                        *\s""" + getNextMessage(10) + "*",
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
            playingSongFromYoutube.setFooter("*The next message will come up in 10 seconds...*");
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
                    queueManipulation.setImage("https://i.imgur.com/1PWkKef.gif");
                    queueManipulation.setFooter(getNextMessage(20));
                    usersInTutorial.remove(user);
                    msg.replyEmbeds(queueManipulation.build()).queueAfter(10, TimeUnit.SECONDS);
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
        return "The next message will come up in" + seconds + " seconds...";
    }

    public static boolean userInTutorial(User user) {
        return usersInTutorial.contains(user);
    }
}
