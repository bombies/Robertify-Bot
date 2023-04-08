package main.commands.contextcommands.music;

import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager;
import lombok.extern.slf4j.Slf4j;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.slashcommands.commands.audio.PlaySlashCommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractContextCommand;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class SearchContextCommand extends AbstractContextCommand {
    @Override
    protected ContextCommandData buildCommand() {
        return getBuilder()
                .name("Search For Track")
                .type(Command.Type.MESSAGE)
                .guildOnly(true)
                .build();
    }

    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent event) {
        if (!checks(event))
            return;

        final var guild = event.getGuild();
        final var memberVoiceState = event.getMember().getVoiceState();
        final var selfVoiceState = event.getMember().getVoiceState();

        if (!memberVoiceState.inAudioChannel()) {
            final var eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.USER_VOICE_CHANNEL_NEEDED);
            event.replyEmbeds(eb.build())
                    .setEphemeral(true).queue();
            return;
        }

        if (selfVoiceState.inAudioChannel() && !memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()))
                            .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.deferReply().queue();

        var query = event.getTarget().getContentRaw();
        if (!GeneralUtils.isUrl(query))
            query = SpotifySourceManager.SEARCH_PREFIX + query;

        if (PlaySlashCommand.isYouTubeLink(query)) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NO_YOUTUBE_SUPPORT).build())
                    .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getGuildChannel()))
                    .queue();
            return;
        }

        String finalQuery = query;
        event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.FT_ADDING_TO_QUEUE_2).build())
                .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()))
                .queue(msg ->  {
                    RobertifyAudioManager.getInstance()
                            .loadAndPlay(
                                    finalQuery,
                                    selfVoiceState,
                                    memberVoiceState,
                                    msg,
                                    event,
                                    false
                            );
                });
    }
}
