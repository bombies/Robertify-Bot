package main.commands.slashcommands.commands.dev.test;

import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.autoplay.AutoPlayUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

public class SpotifyRecommendationTest extends AbstractSlashCommand {
    @Override
    protected void buildCommand() {
        setCommand(getBuilder()
                .setName("recommendationtest")
                .setDescription("Test Spotify recommendations")
                .build());
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!devCheck(event)) return;

        final var recommendations = AutoPlayUtils.getSpotifyRecommendations("2Gzy8TYJ5xrEMDyUjZuDsK", "dancehall,reggae", "53rvxSNmKhy7wMAAygyYWr");
        final Guild guild = event.getGuild();
//        final List<MessageEmbed> splitMessage = new ArrayList<>();
//
//        String message = Arrays.toString(recommendations.getTracks());
//
//        if (message.length() > 4090) {
//            int startIndex = 0;
//            while (message.length() > 4090) {
//                splitMessage.add(RobertifyEmbedUtils.embedMessage(guild, "```" + message.substring(startIndex, startIndex + 4089) + "```").build());
//                startIndex += 4089;
//                message = message.substring(startIndex, startIndex + 4089);
//            }
//
//            if (!message.isBlank() || !message.isEmpty())
//                splitMessage.add(RobertifyEmbedUtils.embedMessage(guild, "```" + message + "```").build());
//        } else {
//            splitMessage.add(RobertifyEmbedUtils.embedMessage(guild, "```" + message + "```").build());
//        }
//
//
//        event.reply("Fetched!").queue();
//        for (final var embed : splitMessage)
//            event.getChannel().asTextChannel().sendMessageEmbeds(embed).queue();

        var str = new StringBuilder("");
        for (final var track : recommendations.getTracks())
            str.append(track.getName()).append(" - ").append(track.getArtists()[0].getName()).append("\n");
        event.reply(str.toString()).queue();
    }
}
