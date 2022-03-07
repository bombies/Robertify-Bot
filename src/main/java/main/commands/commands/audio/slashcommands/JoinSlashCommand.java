package main.commands.commands.audio.slashcommands;

import main.commands.commands.audio.JoinCommand;
import main.utils.component.legacy.InteractiveCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JoinSlashCommand extends InteractiveCommand {
    private final String commandName = new JoinCommand().getName();

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
                        "Force the bot to join the voice channel you're currently in",
                        List.of(),
                        djPredicate
                )).build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(commandName)) return;

        event.deferReply().queue();

        event.getHook().sendMessageEmbeds(new JoinCommand().handleJoin(
                event.getGuild(),
                event.getTextChannel(),
                event.getMember().getVoiceState(),
                event.getGuild().getSelfMember().getVoiceState()
        )).queue();
    }
}
