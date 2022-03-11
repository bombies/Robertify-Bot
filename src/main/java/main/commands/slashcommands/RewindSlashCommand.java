package main.commands.slashcommands;

import main.commands.commands.audio.RewindCommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.component.legacy.InteractiveCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RewindSlashCommand extends AbstractSlashCommand {

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("rewind")
                        .setDescription("Rewind the song by the seconds provided or all the way to the beginning")
                        .addOptions(
                                CommandOption.of(
                                        OptionType.INTEGER,
                                        "seconds",
                                        "Seconds to rewind the song by",
                                        false
                                )
                        )
                        .setPossibleDJCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checks(event)) return;

        event.deferReply().queue();

        if (!musicCommandDJCheck(event)) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "You need to be a DJ to run this command!").build())
                    .queue();
            return;
        }

        long time = -1;

        if (!event.getOptions().isEmpty()) {
            time = event.getOption("seconds").getAsLong();
        }

        event.getHook().sendMessageEmbeds(new RewindCommand().handleRewind(event.getUser(), event.getGuild().getSelfMember().getVoiceState(), time, event.getOptions().isEmpty()).build())
                .queue();
    }
}
