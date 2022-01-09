package main.commands.commands.audio.slashcommands;

import main.commands.commands.audio.RewindCommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.InteractiveCommand;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RewindSlashCommand extends InteractiveCommand {
    private final String commandName = new RewindCommand().getName();

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

    public InteractionCommand getCommand() {
        return InteractionCommand.create()
                .setCommand(Command.of(
                    commandName,
                    "Rewind the song by the seconds provided or all the way to the beginning",
                    List.of(CommandOption.of(
                            OptionType.INTEGER,
                            "seconds",
                            "Seconds to rewind the song by",
                            false
                    )),
                    djPredicate
                )).build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(commandName)) return;

        event.deferReply().queue();

        if (!getCommand().getCommand().permissionCheck(event)) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "You need to be a DJ to run this command!").build())
                    .queue();
            return;
        }

        long time = -1;

        if (!event.getOptions().isEmpty()) {
            time = event.getOption("seconds").getAsLong();
        }

        event.getHook().sendMessageEmbeds(new RewindCommand().handleRewind(event.getGuild().getSelfMember().getVoiceState(), time, event.getOptions().isEmpty()).build())
                .queue();
    }
}
