package main.commands.slashcommands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.audio.ClearQueueCommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
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
        return "Clear all the queued songs";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;
        sendRandomMessage(event);

        event.deferReply().queue();

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
        final var queue = musicManager.getScheduler().queue;
        final var guild = event.getGuild();
        final var localeManager = LocaleManager.getLocaleManager(guild);

        if (queue.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.ClearQueueMessages.CQ_NOTHING_IN_QUEUE));
            event.getHook().sendMessageEmbeds(eb.build())
                    .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getGuildChannel()))
                    .queue();
            return;
        }

        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();

        if (selfVoiceState.inAudioChannel()) {
            if (selfVoiceState.getChannel().getMembers().size() > 2) {
                if (!musicCommandDJCheck(event)) {
                    EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.ClearQueueMessages.DJ_PERMS_NEEDED));
                    event.getHook().sendMessageEmbeds(eb.build())
                            .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getGuildChannel()))
                            .queue();
                    return;
                }
            }
        } else {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.NO_VOICE_CHANNEL));
            event.getHook().sendMessageEmbeds(eb.build())
                    .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getGuildChannel()))
                    .queue();
            return;
        }

        queue.clear();
        new LogUtils(guild).sendLog(LogType.QUEUE_CLEAR, event.getUser().getAsMention() + " " + localeManager.getMessage(RobertifyLocaleMessage.ClearQueueMessages.QUEUE_CLEARED_USER));

        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.ClearQueueMessages.QUEUE_CLEAR));
        event.getHook().sendMessageEmbeds(eb.build())
                .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getGuildChannel()))
                .queue();
    }
}
