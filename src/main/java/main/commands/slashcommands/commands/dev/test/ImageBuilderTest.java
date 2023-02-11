package main.commands.slashcommands.commands.dev.test;

import lombok.SneakyThrows;
import main.utils.apis.robertify.imagebuilders.NowPlayingImageBuilder;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

public class ImageBuilderTest extends AbstractSlashCommand {
    @Override
    protected void buildCommand() {
        setCommand(getBuilder()
                .setName("imagebuildertest")
                .setDescription("Test building an image")
                .setDevCommand()
                .build());
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override @SneakyThrows
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!devCheck(event)) return;

        event.deferReply().queue();
        final var image = new NowPlayingImageBuilder()
                .setArtistName("Summer Walker")
                .setTitle("Karma")
                .setAlbumImage("https://cdns-images.dzcdn.net/images/cover/4ba5878f51f5aa6f1995b2ba72878f0a/350x350.jpg")
                .setDuration(50000L)
                .setCurrentTime(25000L)
                .build();
        event.getHook().sendMessage("Image generated").addFiles(FileUpload.fromData(
                image
        )).queue(done -> image.delete());
    }
}
