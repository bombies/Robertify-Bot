package main.commands.slashcommands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.audio.SkipCommand;
import main.commands.prefixcommands.audio.SkipToCommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

public class SkipSlashCommand extends AbstractSlashCommand {

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("skip")
                        .setDescription("Skip the song currently being played")
                        .addOptions(
                                CommandOption.of(
                                        OptionType.INTEGER,
                                        "to",
                                        "Number of tacks to skip",
                                        false
                                )
                        )
                        .setPossibleDJCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Skips a track or skip to a specific song in the queue" +
                "\nUsage: `/skip [id]`";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;

        event.deferReply().queue();

        final var selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
        final var memberVoiceState = event.getMember().getVoiceState();

        if (!musicCommandDJCheck(event)) {
            if (!selfVoiceState.inAudioChannel()) {
                event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), RobertifyLocaleMessage.GeneralMessages.VOICE_CHANNEL_NEEDED).build())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            if (selfVoiceState.getChannel().getMembers().size() > 2) {
                MessageEmbed embed = new SkipCommand().handleVoteSkip(event.getChannel().asGuildMessageChannel(), selfVoiceState, memberVoiceState);
                if (embed != null) {
                    event.getHook().sendMessageEmbeds(embed)
                            .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()))
                            .queue();
                } else {
                    event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), RobertifyLocaleMessage.SkipMessages.VOTE_SKIP_STARTED).build())
                            .setEphemeral(true)
                            .queue();
                }
                sendRandomMessage(event);
                return;
            }
        }

        if (event.getOptions().isEmpty()) {
            event.getHook().sendMessageEmbeds(new SkipCommand().handleSkip(selfVoiceState, memberVoiceState))
                    .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()))
                    .queue();
        } else {
            final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
            final int tracksToSkip = GeneralUtils.longToInt(event.getOption("to").getAsLong());
            event.getHook().sendMessageEmbeds(new SkipToCommand().handleSkip(event.getUser(), musicManager, tracksToSkip).build())
                    .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()))
                    .queue();
        }
        sendRandomMessage(event);
    }
}
