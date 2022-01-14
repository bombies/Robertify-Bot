package main.main;

import api.deezer.DeezerApi;
import com.github.kskelm.baringo.BaringoClient;
import com.github.kskelm.baringo.util.BaringoApiException;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeHttpContextFilter;
import lombok.Getter;
import main.commands.commands.audio.slashcommands.*;
import main.commands.commands.dev.AnnouncementCommand;
import main.commands.commands.management.BanCommand;
import main.commands.commands.management.SetChannelCommand;
import main.commands.commands.management.ThemeCommand;
import main.commands.commands.management.UnbanCommand;
import main.commands.commands.management.dedicatechannel.DedicatedChannelEvents;
import main.commands.commands.management.permissions.ListDJCommand;
import main.commands.commands.management.permissions.RemoveDJCommand;
import main.commands.commands.management.permissions.SetDJCommand;
import main.commands.commands.misc.EightBallCommand;
import main.commands.commands.misc.poll.PollEvents;
import main.commands.commands.util.*;
import main.commands.commands.util.reports.ReportsEvents;
import main.constants.ENV;
import main.events.AnnouncementChannelEvents;
import main.events.SuggestionCategoryDeletionEvents;
import main.events.VoiceChannelEvents;
import main.utils.pagination.PaginationEvents;
import main.utils.spotify.SpotifyAuthorizationUtils;
import me.duncte123.botcommons.web.WebUtils;
import net.dv8tion.jda.api.GatewayEncoding;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Robertify {

    private static final Logger logger = LoggerFactory.getLogger(Robertify.class);

    @Getter
    public static JDA api;
    public static BaringoClient baringo;
    @Getter
    private static DeezerApi deezerApi;
    @Getter
    private static SpotifyApi spotifyApi;
    @Getter
    private static final EventWaiter commandWaiter = new EventWaiter();

    public static void main(String[] args) {
        WebUtils.setUserAgent("Mozilla/Robertify / bombies#4445");

        try {
            api = JDABuilder.createDefault(
                            Config.get(ENV.BOT_TOKEN),
                            GatewayIntent.GUILD_VOICE_STATES,
                            GatewayIntent.GUILD_MESSAGE_REACTIONS,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.DIRECT_MESSAGES
                    )
                    .setAudioSendFactory(new NativeAudioSendFactory())
                    .setChunkingFilter(ChunkingFilter.NONE)

                    // Event Listeners
                    .addEventListeners(
                            VoiceChannelEvents.waiter,
                            commandWaiter,
                            new Listener(commandWaiter),
                            new VoiceChannelEvents(),
                            new DedicatedChannelEvents(),
                            new PollEvents(),
                            new SuggestionCategoryDeletionEvents(),
                            new ReportsEvents(),
                            new AnnouncementChannelEvents()
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
                            new RemoveDJCommand(),
                            new SeekSlashCommand(),
                            new JoinSlashCommand(),
                            new BanCommand(),
                            new UnbanCommand(),
                            new ShuffleSlashCommand(),
                            new EightBallCommand(),
                            new SuggestionCommand(),
                            new ListDJCommand(),
                            new LofiSlashCommand(),
                            new UptimeCommand(),
                            new SupportServerCommand(),
                            new ShufflePlaySlashCommand(),
                            new VoteCommand(),
                            new DonateCommand(),
                            new ThemeCommand()
                    )

                    // Button Listeners
                    .addEventListeners(
                            new PaginationEvents(),
                            new AnnouncementCommand()
                    )

                    .enableCache(
                            CacheFlag.VOICE_STATE
                    )
                    .disableCache(
                            CacheFlag.ACTIVITY,
                            CacheFlag.EMOTE,
                            CacheFlag.CLIENT_STATUS,
                            CacheFlag.ROLE_TAGS,
                            CacheFlag.ONLINE_STATUS
                    )
                    .setGatewayEncoding(GatewayEncoding.ETF)
                    .setActivity(Activity.listening("Starting up..."))
                    .build();

            YoutubeHttpContextFilter.setPAPISID(Config.get(ENV.YOUTUBE_PAPISID));
            YoutubeHttpContextFilter.setPSID(Config.get(ENV.YOUTUBE_PSID));

            spotifyApi = new SpotifyApi.Builder()
                    .setClientId(Config.get(ENV.SPOTIFY_CLIENT_ID))
                    .setClientSecret(Config.get(ENV.SPOTIFY_CLIENT_SECRET))
                    .setRedirectUri(SpotifyHttpManager.makeUri("http://localhost/callback/"))
                    .build();

            deezerApi = new DeezerApi();

            final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(SpotifyAuthorizationUtils.doTokenRefresh(), 0, 1, TimeUnit.HOURS);

            try {
                baringo = new BaringoClient.Builder()
                        .clientAuth(Config.get(ENV.IMGUR_CLIENT), Config.get(ENV.IMGUR_SECRET))
                        .build();
            } catch (BaringoApiException e) {
                logger.error("[ERROR] There was an issue building the Baringo client!", e);
            }
        } catch (Exception e) {
            logger.error("[FATAL ERROR] An unexpected error occurred!", e);
        }
    }
}


