package main.main;

import com.github.kskelm.baringo.BaringoClient;
import com.github.kskelm.baringo.util.BaringoApiException;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeHttpContextFilter;
import lombok.Getter;
import main.commands.commands.audio.slashcommands.*;
import main.commands.commands.management.BanCommand;
import main.commands.commands.management.SetChannelCommand;
import main.commands.commands.management.UnbanCommand;
import main.commands.commands.management.dedicatechannel.DedicatedChannelEvents;
import main.commands.commands.management.permissions.ListDJCommand;
import main.commands.commands.management.permissions.RemoveDJCommand;
import main.commands.commands.management.permissions.SetDJCommand;
import main.commands.commands.misc.EightBallCommand;
import main.commands.commands.misc.poll.PollEvents;
import main.commands.commands.util.HelpCommand;
import main.commands.commands.util.SuggestionCommand;
import main.commands.commands.util.SupportServerCommand;
import main.commands.commands.util.UptimeCommand;
import main.commands.commands.util.reports.ReportsEvents;
import main.constants.ENV;
import main.events.SuggestionCategoryDeletionEvents;
import main.events.VoiceChannelEvents;
import main.utils.GeneralUtils;
import main.utils.pagination.PaginationEvents;
import main.utils.spotify.SpotifyAuthorizationUtils;
import me.duncte123.botcommons.web.WebUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
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
                            GatewayIntent.GUILD_MESSAGE_REACTIONS,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.GUILD_EMOJIS,
                            GatewayIntent.GUILD_PRESENCES,
                            GatewayIntent.DIRECT_MESSAGES
                    )
                    .setAudioSendFactory(new NativeAudioSendFactory())
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .setChunkingFilter(ChunkingFilter.ALL)

                    // Event Listeners
                    .addEventListeners(
                            VoiceChannelEvents.waiter,
                            commandWaiter,
                            new Listener(commandWaiter),
                            new VoiceChannelEvents(),
                            new DedicatedChannelEvents(),
                            new PollEvents(),
                            new SuggestionCategoryDeletionEvents(),
                            new ReportsEvents()
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
                            new SupportServerCommand()
                    )

                    // Button Listeners
                    .addEventListeners(new PaginationEvents())

                    .enableCache(
                            CacheFlag.VOICE_STATE,
                            CacheFlag.MEMBER_OVERRIDES,
                            CacheFlag.ONLINE_STATUS
                    )
                    .build();

            YoutubeHttpContextFilter.setPAPISID(Config.get(ENV.YOUTUBE_PAPISID));
            YoutubeHttpContextFilter.setPSID(Config.get(ENV.YOUTUBE_PSID));

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
                logger.error("[ERROR] There was an issue building the Baringo client!", e);
            }
        } catch (Exception e) {
            logger.error("[FATAL ERROR] An unexpected error occurred!", e);
        }
    }

    private static class RefreshSpotifyToken implements Runnable {

        @Override
        public void run() {
            SpotifyAuthorizationUtils.setTokens();
        }
    }
}


