package main.commands.slashcommands.commands.dev;

import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.spotify.SpotifyAuthorizationUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

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

        SpotifyAuthorizationUtils.setTokens();
        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "Refreshed Spotify token!").build())
                .setEphemeral(true)
                .queue();
    }
}
