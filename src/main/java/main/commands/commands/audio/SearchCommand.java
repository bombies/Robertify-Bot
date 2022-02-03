package main.commands.commands.audio;

import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.InteractiveCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.List;

public class SearchCommand extends InteractiveCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var guild = ctx.getGuild();
        final var user = ctx.getAuthor();
        final var msg = ctx.getMessage();
        final var args = ctx.getArgs();

        if (args.isEmpty()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a query!").build())
                    .queue();
            return;
        }

        final String query = String.join(" ", args);

        msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Now looking for: `" + query + "`").build())
                .queue(searchingMsg -> getSearchResults(guild, user, searchingMsg, "ytsearch:" + query));
    }

    private void getSearchResults(Guild guild, User requester, Message botMsg, String query) {
        GuildMusicManager musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        RobertifyAudioManager.getInstance()
                .loadSearchResults(musicManager, requester, botMsg, query);
    }

    @Override
    public String getName() {
        return "search";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

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
                        getName(),
                        "Search and browse for a specific track!",
                        List.of(
                                CommandOption.of(
                                        OptionType.STRING,
                                        "query",
                                        "What would you like to search for?",
                                        true
                                )
                        ),
                        djPredicate
                ))
                .build();
    }

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        if (!event.getComponentId().startsWith("searchresult:")) return;

        String[] split = event.getComponentId().split(":");
        final String id = split[1];

        Guild guild = event.getGuild();
        if (!event.getUser().getId().equals(id)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You do not have permission to interact with this menu!").build())
                    .setEphemeral(true)
                    .queue();
        }

        GuildVoiceState voiceState = guild.getSelfMember().getVoiceState();
        GuildVoiceState memberVoiceState = event.getMember().getVoiceState();

        if (!memberVoiceState.inVoiceChannel()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be in a voice channel to use this!").build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (voiceState.inVoiceChannel() && (!voiceState.getChannel().equals(memberVoiceState.getChannel()))) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this!").build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final var trackID = event.getSelectedOptions().get(0).getValue();

        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Adding that track to the queue!").build())
                .setEphemeral(true)
                .queue();

        RobertifyAudioManager.getInstance().loadAndPlay(event.getTextChannel(), "ytsearch:" + trackID,
                voiceState, memberVoiceState, event.getMessage(), false);
    }
}
