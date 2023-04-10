package main.commands.slashcommands.commands.audio;

import main.commands.prefixcommands.audio.JoinCommand;
import main.constants.Permission;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public class JoinSlashCommand extends AbstractSlashCommand {

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("join")
                        .setDescription("Force the bot to join the voice channel you're currently in")
                        .setPossibleDJCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return """
                Use this command to forcefully move the bot into your voice channel.

                *NOTE: This command can be made DJ only by using* `toggles dj join`""";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;
        sendRandomMessage(event);

        event.deferReply().queue();

        if (!musicCommandDJCheck(event)) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), RobertifyLocaleMessage.GeneralMessages.INSUFFICIENT_PERMS, Pair.of("{permissions}", Permission.ROBERTIFY_DJ.name().toUpperCase()))
                    .build())
                    .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()))
                    .queue();
            return;
        }

        event.getHook().sendMessageEmbeds(new JoinCommand().handleJoin(
                event.getGuild(),
                event.getMember().getVoiceState(),
                event.getGuild().getSelfMember().getVoiceState()
        )).queue();
    }
}
