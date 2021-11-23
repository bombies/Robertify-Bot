package main.main;

import com.github.kskelm.baringo.BaringoClient;
import com.github.kskelm.baringo.util.BaringoApiException;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import lombok.Getter;
import main.commands.commands.audio.ClearQueueCommand;
import main.commands.commands.audio.slashcommands.*;
import main.commands.commands.management.SetChannelCommand;
import main.commands.commands.management.permissions.RemoveDJCommand;
import main.commands.commands.management.permissions.SetDJCommand;
import main.commands.commands.util.HelpCommand;
import main.constants.ENV;
import main.events.VoiceChannelEvents;
import main.utils.GeneralUtils;
import main.utils.pagination.PaginationEvents;
import main.utils.spotify.SpotifyAuthorizationUtils;
import me.duncte123.botcommons.web.WebUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Robertify {

    public static JDA api;
    public static BaringoClient baringo;
    @Getter
    private static SpotifyApi spotifyApi;
    @Getter
    private static final EventWaiter commandWaiter = new EventWaiter();

    public static void main(String[] args) {
        WebUtils.setUserAgent("Mozilla/Robertify / bombies#4445");
        GeneralUtils.setDefaultEmbed();

        try {

            api = JDABuilder.createDefault(
                            Config.get(ENV.BOT_TOKEN),
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.GUILD_VOICE_STATES,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.GUILD_EMOJIS,
                            GatewayIntent.DIRECT_MESSAGES,
                            GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                            GatewayIntent.GUILD_MESSAGE_REACTIONS,
                            GatewayIntent.GUILD_PRESENCES
                    )
                    .setChunkingFilter(ChunkingFilter.ALL)

                    // Event Listeners
                    .addEventListeners(
                            VoiceChannelEvents.waiter,
                            commandWaiter,
                            new Listener(commandWaiter),
                            new VoiceChannelEvents()
                    )

                    // Slash Commands
                    .addEventListeners(
                            new PlaySlashCommand(),
                            new QueueSlashCommand(),
                            new LeaveSlashCommand(),
                            new ClearQueueSlashCommand(),
                            new JumpSlashCommand(),
                            new NowPlayingSlashCommand(),
                            new PauseSlashCommand(),
                            new HelpCommand(),
                            new SkipSlashCommand(),
                            new RemoveSlashCommand(),
                            new LoopSlashCommand(),
                            new MoveSlashCommand(),
                            new RewindSlashCommand(),
                            new SetChannelCommand(),
                            new VolumeSlashCommand(),
                            new SetDJCommand(),
                            new RemoveDJCommand()
                    )

                    // Button Listeners
                    .addEventListeners(new PaginationEvents())

                    .enableCache(
                            CacheFlag.ACTIVITY,
                            CacheFlag.CLIENT_STATUS,
                            CacheFlag.VOICE_STATE,
                            CacheFlag.ONLINE_STATUS,
                            CacheFlag.MEMBER_OVERRIDES
                    )
                    .build();

            spotifyApi = new SpotifyApi.Builder()
                    .setClientId(Config.get(ENV.SPOTIFY_CLIENT_ID))
                    .setClientSecret(Config.get(ENV.SPOTIFY_CLIENT_SECRET))
                    .setRedirectUri(SpotifyHttpManager.makeUri("http://localhost/callback/"))
                    .build();

            final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(new RefreshSpotifyToken(), 0, 1, TimeUnit.HOURS);

            try {
                baringo = new BaringoClient.Builder()
                        .clientAuth(Config.get(ENV.IMGUR_CLIENT), Config.get(ENV.IMGUR_SECRET))
                        .build();
            } catch (BaringoApiException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class RefreshSpotifyToken implements Runnable {

        @Override
        public void run() {
            SpotifyAuthorizationUtils.setTokens();
        }
    }
}


