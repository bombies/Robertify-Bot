package main.main;

import main.commands.commands.audio.QueueCommand;
import main.constants.ENV;
import main.events.VoiceChannelEvents;
import main.utils.GeneralUtils;
import main.utils.pagination.PaginationEvents;
import me.duncte123.botcommons.web.WebUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class Robertify {

    public static JDA api;

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
                            new Listener(),
                            new VoiceChannelEvents()
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
