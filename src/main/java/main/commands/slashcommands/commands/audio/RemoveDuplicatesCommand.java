package main.commands.slashcommands.commands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.RobertifyAudioManager;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentLinkedQueue;

public class RemoveDuplicatesCommand extends AbstractSlashCommand {
    @Override
    protected void buildCommand() {
        setCommand(getBuilder()
                .setName("removedupes")
                .setDescription("Remove all duplicate tracks in the queue")
                .build());
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;

        assert event.getGuild() != null;
        final var guild = event.getGuild();
        final var queueHandler = RobertifyAudioManager.getInstance().getMusicManager(guild)
                .getScheduler()
                .getQueueHandler();

        if (queueHandler.isEmpty()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NOTHING_IN_QUEUE).build())
                    .queue();
            return;
        }

        event.deferReply().queue();

        final var newQueue = new ConcurrentLinkedQueue<AudioTrack>();
        for (final var track : queueHandler.contents())
            if (newQueue.stream().noneMatch(t -> t.getIdentifier().equals(track.getIdentifier())))
                newQueue.add(track);
        queueHandler.clear();
        queueHandler.addAll(newQueue);

        event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.DuplicateMessages.REMOVED_DUPLICATES).build())
                .queue();
    }

    @Override
    public String getHelp() {
        return null;
    }
}
