package main.commands.commands.misc;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.TimeFormat;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.InteractiveCommand;
import main.utils.database.mongodb.cache.BotInfoCache;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.HashMap;
import java.util.List;

public class PlaytimeCommand extends InteractiveCommand implements ICommand {
    public static HashMap<Long, Long> playtime = new HashMap<>();

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var guild = ctx.getGuild();
        final var msg = ctx.getMessage();

        msg.replyEmbeds(handlePlaytime(guild)).queue();
    }

    private MessageEmbed handlePlaytime(Guild guild) {
        final var audioPlayer = RobertifyAudioManager.getInstance()
                .getMusicManager(guild)
                .getPlayer();

        final var time = (playtime.get(guild.getIdLong()) == null ? 0 : playtime.get(guild.getIdLong())) + (audioPlayer.getPlayingTrack() == null ? 0 : audioPlayer.getTrackPosition());

        return RobertifyEmbedUtils.embedMessage(guild, "🎶 I have listened to **"+ GeneralUtils.getDurationString(time)+"** of music in this guild since my last boot.")
                .setFooter("Last booted: " + GeneralUtils.formatDate(System.currentTimeMillis() - BotInfoCache.getInstance().getLastStartup(), TimeFormat.E_DD_MMM_YYYY_HH_MM_SS_Z)).build();
    }

    @Override
    public String getName() {
        return "playtime";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return List.of("pt");
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
                        "See how long the bot has played music in this guild since its last startup!"
                ))
                .build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(getName()))
            return;

        event.replyEmbeds(handlePlaytime(event.getGuild())).queue();
    }
}
