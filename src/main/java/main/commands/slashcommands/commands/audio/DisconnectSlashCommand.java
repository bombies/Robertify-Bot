package main.commands.slashcommands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

public class DisconnectSlashCommand extends AbstractSlashCommand {
    private final String commandName = "disconnect";

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName(commandName)
                        .setDescription("Disconnect the bot from the voice channel it's currently in")
                        .setPossibleDJCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Forces the bot to stop playing music and leave the voice channel" +
                " if already in one.";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;
        sendRandomMessage(event);

        event.deferReply().queue();

        EmbedBuilder eb;

        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        Guild guild = event.getGuild();
        final GuildVoiceState selfVoiceState = guild.getSelfMember().getVoiceState();

        if (!selfVoiceState.inAudioChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "I'm already not in a voice channel!");
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.inAudioChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfVoiceState.getChannel().getAsMention());
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfVoiceState.getChannel().getAsMention());
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);

        musicManager.leave();
        new LogUtils().sendLog(guild, LogType.BOT_DISCONNECTED, event.getUser().getAsMention() + " has disconnected the bot.");

        eb = RobertifyEmbedUtils.embedMessage(guild, "Disconnected!");
        event.getHook().sendMessageEmbeds(eb.build()).queue();
    }
}
