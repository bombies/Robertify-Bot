package main.commands.commands.audio.slashcommands;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.commands.audio.LofiCommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.InteractiveCommand;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

public class LofiSlashCommand extends InteractiveCommand {
    private final String commandName = new LofiCommand().getName();

    @Override
    public void initCommand() {
        setInteractionCommand(getCommand());
        upsertCommand();
    }

    @Override
    public void initCommand(Guild g) {
        setInteractionCommand(getCommand());
        upsertCommand(g);
    }

    private InteractionCommand getCommand() {
        return InteractionCommand.create()
                .setCommand(Command.of(
                        commandName,
                        "Play chill beats to relax/study to",
                        djPredicate
                )).build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(commandName)) return;

        event.deferReply().queue();

        try {
            event.getHook().sendMessageEmbeds(new LofiCommand().handleLofi(event.getGuild(), event.getMember(), event.getTextChannel()))
                    .queue();
        } catch (IllegalArgumentException e) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "Enabling Lo-Fi mode...").build())
                    .queue(botMsg -> {
                        LofiCommand.getLofiEnabledGuilds().add(event.getGuild().getIdLong());
                        LofiCommand.getAnnounceLofiMode().add(event.getGuild().getIdLong());
                        RobertifyAudioManager.getInstance()
                                .loadAndPlayFromDedicatedChannel(
                                        "https://www.youtube.com/watch?v=5qap5aO4i9A&ab_channel=LofiGirl",
                                        event.getGuild().getSelfMember().getVoiceState(),
                                        event.getMember().getVoiceState(),
                                        botMsg,
                                        event
                                );
                    });
        }
    }
}
