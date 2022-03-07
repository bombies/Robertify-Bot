package main.commands.commands.audio.slashcommands;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.commands.audio.ClearQueueCommand;
import main.constants.Permission;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.legacy.InteractiveCommand;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

public class ClearQueueSlashCommand extends InteractiveCommand {
    private final String commandName = new ClearQueueCommand().getName();

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
                        "Clear the queue of all its contents",
                        e -> GeneralUtils.hasPerms(e.getGuild(), e.getMember(), Permission.ROBERTIFY_DJ)
                ))
                .build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(commandName)) return;

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
                if (!getInteractionCommand().getCommand().permissionCheck(event)) {
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
