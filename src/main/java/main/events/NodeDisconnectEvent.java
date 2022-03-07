package main.events;

import lavalink.client.io.jda.JdaLavalink;
import lavalink.client.io.jda.JdaLink;
import main.main.Robertify;

public class NodeDisconnectEvent {
    private final static JdaLavalink node = Robertify.getLavalink();

    public static void reconnect() {
        for (final var guild : Robertify.api.getGuilds()) {
            JdaLink link = node.getLink(guild);

            link.onVoiceServerUpdate(link.getLastVoiceServerUpdate(), "");
        }
    }
}
