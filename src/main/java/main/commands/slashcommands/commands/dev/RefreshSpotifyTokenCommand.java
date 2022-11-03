package main.commands.slashcommands.commands.dev;

import main.main.Robertify;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.spotify.SpotifyAuthorizationUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class RefreshSpotifyTokenCommand extends AbstractSlashCommand {
    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("refreshspotifytoken")
                        .setDescription("This command force refreshes the Spotify token")
                        .setDevCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!devCheck(event)) return;

        Robertify.getSpotifyTokenRefreshScheduler().scheduleAtFixedRate(SpotifyAuthorizationUtils.doTokenRefresh(), 0, 1, TimeUnit.HOURS);
        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "Refreshed Spotify token!").build())
                .setEphemeral(true)
                .queue();
    }
}
