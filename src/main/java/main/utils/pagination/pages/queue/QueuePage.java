package main.utils.pagination.pages.queue;

import lombok.Getter;
import main.commands.prefixcommands.audio.QueueCommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.apis.robertify.imagebuilders.QueueImageBuilder;
import main.utils.pagination.pages.MessagePage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class QueuePage extends MessagePage {
    private final Guild guild;
    @Getter
    private final int pageNumber;
    @Getter
    private final List<QueueItem> queueItems;

    public QueuePage(Guild guild, int pageNumber, List<QueueItem> queueItems) {
        this.guild = guild;
        this.pageNumber = pageNumber;
        this.queueItems = queueItems;
    }

    public QueuePage(Guild guild, int pageNumber) {
        this.guild = guild;
        this.pageNumber = pageNumber;
        this.queueItems = new ArrayList<>();
    }

    public void addItem(QueueItem item) {
        queueItems.add(item);
    }

    public void addItem(int trackIndex, String title, String artist, long duration, @Nullable String artworkUrl) {
        queueItems.add(new QueueItem(trackIndex, title, artist, duration, artworkUrl));
    }

    public InputStream getImage() throws SocketTimeoutException, ConnectException {
        final var builder = new QueueImageBuilder()
                .setPage(pageNumber);

        for (final var item : queueItems)
            builder.addTrack(
                    item.getTrackIndex(),
                    item.getTrackTitle(),
                    item.getArtist(),
                    item.getDuration(),
                    item.getArtworkUrl()
            );

        return builder.build();
    }

    @Override
    public MessageEmbed getEmbed() {
        final var content = new QueueCommand().getContent(guild, queueItems);
        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "\t");
        for (String str : content)
            eb.appendDescription(str + "\n");
        return eb.build();
    }
}