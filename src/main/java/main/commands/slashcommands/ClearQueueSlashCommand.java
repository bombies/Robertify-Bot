package main.commands.slashcommands;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.commands.audio.ClearQueueCommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

public class ClearQueueSlashCommand extends AbstractSlashCommand {
    private final String commandName = new ClearQueueCommand().getName();

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName(commandName)
                        .setDescription("Clear the queue of all its contents")
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
        if (!nameCheck(event)) return;

        event.deferReply().queue();

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
        final var queue = musicManager.getScheduler().queue;
        final var guild = event.getGuild();

        GeneralUtils.setCustomEmbed(event.getGuild(), "Queue");

        if (queue.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "There is already nothing in the queue.");
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();

        if (selfVoiceState.inVoiceChannel()) {
            if (selfVoiceState.getChannel().getMembers().size() > 2) {
                if (!musicCommandDJCheck(event)) {
                    EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You need to be a DJ to use this command when there's other users in the channel!");
                    event.getHook().sendMessageEmbeds(eb.build()).queue();
                    return;
                }
            }
        } else {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "The bot isn't in a voice channel.");
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        queue.clear();
        new LogUtils().sendLog(guild, LogType.QUEUE_CLEAR, event.getUser().getAsMention() + " has cleared the queue");


        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "The queue was cleared!");
        event.getHook().sendMessageEmbeds(eb.build()).queue();

        GeneralUtils.setDefaultEmbed(event.getGuild());
    }
}
