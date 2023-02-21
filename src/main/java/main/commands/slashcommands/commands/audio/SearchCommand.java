package main.commands.slashcommands.commands.audio;

import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.requestchannel.RequestChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.internal.utils.tuple.Pair;
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

        final var dedicatedChannelConfig = new RequestChannelConfig(guild);
        if (dedicatedChannelConfig.isChannelSet())
            if (dedicatedChannelConfig.getChannelID() == channel.getIdLong()) {
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.CANT_BE_USED_IN_CHANNEL).build())
                        .queue();
                return;
            }

        if (args.isEmpty()) {
            final var localeManager = LocaleManager.getLocaleManager(guild);
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.SearchMessages.MUST_PROVIDE_QUERY) + "\n\n"
                            + getUsages(new GuildConfig(guild).getPrefix())).build())
                    .queue();
            return;
        }

        final String query = String.join(" ", args);

        msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.SearchMessages.LOOKING_FOR, Pair.of("{query}", query)).build())
                .queue(searchingMsg -> getSearchResults(guild, user, searchingMsg, SpotifySourceManager.SEARCH_PREFIX + query));
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
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;
        sendRandomMessage(event);

        final var guild = event.getGuild();

        final var dedicatedChannelConfig = new RequestChannelConfig(guild);
        if (dedicatedChannelConfig.isChannelSet())
            if (dedicatedChannelConfig.getChannelID() == event.getChannel().asGuildMessageChannel().getIdLong()) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.USER_VOICE_CHANNEL_NEEDED).build())
                        .queue();
                return;
            }

        final String query = event.getOption("query").getAsString();


        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.SearchMessages.LOOKING_FOR, Pair.of("{query}", query)).build())
                .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()))
                .queue(addingMsg -> getSearchResults(guild, event.getUser(), addingMsg, SpotifySourceManager.SEARCH_PREFIX + query));
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!event.getComponentId().startsWith("searchresult:")) return;

        String[] split = event.getComponentId().split(":");
        final String id = split[1];

        Guild guild = event.getGuild();
        if (!event.getUser().getId().equals(id)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NO_MENU_PERMS).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        GuildVoiceState voiceState = guild.getSelfMember().getVoiceState();
        GuildVoiceState memberVoiceState = event.getMember().getVoiceState();

        if (!memberVoiceState.inAudioChannel()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.USER_VOICE_CHANNEL_NEEDED).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (voiceState.inAudioChannel() && (!voiceState.getChannel().equals(memberVoiceState.getChannel()))) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.USER_VOICE_CHANNEL_NEEDED).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final var trackQuery = event.getSelectedOptions().get(0).getValue();

        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.FT_ADDING_TO_QUEUE).build())
                .setEphemeral(true)
                .queue();

        RobertifyAudioManager.getInstance().loadAndPlay(event.getChannel().asGuildMessageChannel(), trackQuery,
                voiceState, memberVoiceState, event.getMessage(), false);
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.getButton().getId().startsWith("searchresult:")) return;

        String[] split = event.getButton().getId().split(":");
        String id = split[1], searcherID = split[2];
        switch (id.toLowerCase()) {
            case "end" -> {
                if (!event.getUser().getId().equals(searcherID))
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), RobertifyLocaleMessage.GeneralMessages.NO_PERMS_END_INTERACTION).build())
                            .setEphemeral(true)
                            .queue();
                else
                    event.getMessage().delete().queue();
            }
            default -> throw new IllegalArgumentException("How did this even happen? (ID="+id+")");
        }
    }
}
