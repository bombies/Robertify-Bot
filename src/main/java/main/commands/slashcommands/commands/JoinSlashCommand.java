package main.commands.slashcommands.commands;

import main.commands.commands.audio.JoinCommand;
import main.constants.BotConstants;
import main.constants.Permission;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
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
        return null;
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checks(event)) return;

        event.deferReply().queue();

        if (!musicCommandDJCheck(event)) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), BotConstants.getInsufficientPermsMessage(Permission.ROBERTIFY_DJ))
                    .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.getHook().sendMessageEmbeds(new JoinCommand().handleJoin(
                event.getGuild(),
                event.getTextChannel(),
                event.getMember().getVoiceState(),
                event.getGuild().getSelfMember().getVoiceState()
        )).queue();
    }
}
