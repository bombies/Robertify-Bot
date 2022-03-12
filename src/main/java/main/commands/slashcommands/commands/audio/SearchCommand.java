package main.commands.slashcommands.commands.audio;

import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;

public class SearchCommand extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var guild = ctx.getGuild();
        final var user = ctx.getAuthor();
        final var channel = ctx.getChannel();
        final var msg = ctx.getMessage();
        final var args = ctx.getArgs();

        if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
            if (new DedicatedChannelConfig().getChannelID(guild.getIdLong()) == channel.getIdLong()) {
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "This command cannot be used in this channel!").build())
                        .queue();
                return;
            }

        if (args.isEmpty()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a query!\n\n"
                            + getUsages(new GuildConfig().getPrefix(guild.getIdLong()))).build())
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

    private void getSearchResults(Guild guild, User requester, InteractionHook botMsg, String query) {
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
        return "Search for a specific song! You will be provided a list of maximum 10" +
                " results from our library for you to choose from. It's as easy as selecting" +
                " one of them from the selection menu and it'll be added to the queue!\n\n" +
                getUsages(prefix);
    }

    @Override
    public String getUsages(String prefix) {
        return "**__Usages__**\n" +
                "`"+prefix+"search <query>`";
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("search")
                        .setDescription("Search and browse for a specific track!")
                        .addOptions(
                                CommandOption.of(
                                        OptionType.STRING,
                                        "query",
                                        "What would you like to search for?",
                                        true
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
        sendRandomMessage(event);

        final var guild = event.getGuild();

        if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
            if (new DedicatedChannelConfig().getChannelID(guild.getIdLong()) == event.getTextChannel().getIdLong()) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "This command cannot be used in this channel!").build())
                        .queue();
                return;
            }

        final String query = event.getOption("query").getAsString();


        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Now looking for: `" + query + "`").build())
                .setEphemeral(false)
                .queue(addingMsg -> getSearchResults(guild, event.getUser(), addingMsg, "ytsearch:" + query));
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
            return;
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

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        if (!event.getButton().getId().startsWith("searchresult:")) return;

        String id = event.getButton().getId().split(":")[1];
        switch (id.toLowerCase()) {
            case "end" -> event.getMessage().delete().queue();
            default -> throw new IllegalArgumentException("How did this even happen? (ID="+id+")");
        }
    }
}
