package main.commands.commands.audio.slashcommands;

import main.commands.commands.audio.NowPlayingCommand;
import main.utils.component.InteractiveCommand;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

public class NowPlayingSlashCommand extends InteractiveCommand {
    private final String commandName = "nowplaying";

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
                        "See the song that is currently being played",
                        djPredicate
                )).build();

    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(commandName)) return;

        event.deferReply().queue();

        if (!getCommand().getCommand().permissionCheck(event)) {
            event.getHook().sendMessageEmbeds(EmbedUtils.embedMessage("You need to be a DJ to run this command!").build())
                    .queue();
            return;
        }

        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
        EmbedBuilder eb;

        event.getHook().sendMessageEmbeds(new NowPlayingCommand().getNowPlayingEmbed(event.getGuild(), selfVoiceState, memberVoiceState).build())
                .setEphemeral(false)
                .queue();
    }
}
