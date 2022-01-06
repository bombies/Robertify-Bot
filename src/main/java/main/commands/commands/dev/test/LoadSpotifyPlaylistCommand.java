package main.commands.commands.dev.test;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.SneakyThrows;
import main.audiohandlers.RobertifyAudioManager;
import main.audiohandlers.RobertifyAudioReference;
import main.commands.CommandContext;
import main.commands.ITestCommand;
import main.utils.GeneralUtils;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class LoadSpotifyPlaylistCommand implements ITestCommand {
    @Override @SneakyThrows
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        long startTime = System.currentTimeMillis();

        if (args.isEmpty()) {
            msg.reply("Provide args").queue();
            return;
        }

        var audioRef = new RobertifyAudioReference(args.get(0), null);

        List<AudioTrack> loadedTracks = new ArrayList<>();

        Future<Void> voidFuture = RobertifyAudioManager.getInstance().getAudioPlayerManager()
                .loadItemOrdered(RobertifyAudioManager.getInstance().getMusicManager(ctx.getGuild()),
                        audioRef.identifier, new AudioLoadResultHandler() {
                            @Override
                            public void trackLoaded(AudioTrack audioTrack) {
                                ctx.getChannel().sendMessage("Loaded track").queue();
                                loadedTracks.add(audioTrack);
                            }

                            @Override
                            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                                ctx.getChannel().sendMessage("Loaded playlist").queue();
                                loadedTracks.addAll(audioPlaylist.getTracks());
                            }

                            @Override
                            public void noMatches() {
                                msg.reply("No matches").queue();
                            }

                            @Override
                            public void loadFailed(FriendlyException e) {
                                e.printStackTrace();
                                msg.reply("Load failed").queue();
                            }
                        });

        while (!voidFuture.isDone());
        try {
            msg.reply(loadedTracks.size() + " Tracks loaded. Took " + (System.currentTimeMillis() - startTime) + "ms\n\n" + loadedTracks).queue();
        } catch (IllegalArgumentException e) {
            File file = new File("playlisttest.txt");
            file.createNewFile();
            GeneralUtils.setFileContent(file, loadedTracks.size() + " Tracks loaded. Took " + (System.currentTimeMillis() - startTime) + "ms\n\n" + loadedTracks);
            msg.reply("Result: ").addFile(file).queue();
        }
    }

    @Override
    public String getName() {
        return "loadspotifyplaylist";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return List.of("lsp");
    }
}
