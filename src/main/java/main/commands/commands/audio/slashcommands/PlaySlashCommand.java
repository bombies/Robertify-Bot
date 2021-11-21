package main.commands.commands.audio.slashcommands;

import lombok.SneakyThrows;
import main.audiohandlers.PlayerManager;
import main.commands.commands.audio.PlayCommand;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.component.InteractiveCommand;
import main.utils.database.BotUtils;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

public class PlaySlashCommand extends InteractiveCommand {
    @Override @SneakyThrows
    public void initCommand() {
        var command = InteractiveCommand.InteractionCommand.create()
                .buildCommand()
                    .setName(new PlayCommand().getName())
                    .setDescription("Play a song! Links are accepted by either Spotify or YouTube")
                    .addOption(CommandOption.of(OptionType.STRING, "track", "The name/link of a track", true))
                    .build()
                .build();

        setInteractionCommand(command);
        upsertCommand();
    }

    @Override @SneakyThrows
    public void initCommand(Guild g) {
        var command = InteractiveCommand.InteractionCommand.create()
                .buildCommand()
                .setName(new PlayCommand().getName())
                .setDescription("Play a song! Links are accepted by either Spotify or YouTube")
                .addOption(CommandOption.of(OptionType.STRING, "track", "The name/link of a track", true))
                .build()
                .build();

        setInteractionCommand(command);
        upsertCommand(g);
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(new PlayCommand().getName())) return;

        EmbedBuilder eb;

        BotUtils botUtils = new BotUtils();
        if (!botUtils.isAnnouncementChannelSet(event.getGuild().getIdLong())) {
            botUtils.createConnection();
            botUtils.setAnnouncementChannel(event.getGuild().getIdLong(), event.getChannel().getIdLong())
                    .closeConnection();

            eb = EmbedUtils.embedMessage("There was no announcement channel set! Setting it to this channel.\n" +
                    "\n_You can change the announcement channel by using the \"setchannel\" command._");
            event.getChannel().sendMessageEmbeds(eb.build()).queue();
        }

        final Member member = event.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        if (!memberVoiceState.inVoiceChannel()) {
            eb = EmbedUtils.embedMessage("You need to be in a voice channel for this to work");
            event.replyEmbeds(eb.build()).setEphemeral(false).queue();
            return;
        }

        botUtils.createConnection();

        String link = event.getOption("track").getAsString();

        if (!GeneralUtils.isUrl(link)) {
            link = "ytsearch:" + link;
        }

        PlayerManager.getInstance()
                .loadAndPlay(
                        link,
                        botUtils.getAnnouncementChannelObject(event.getGuild().getIdLong()),
                        event.getGuild().getSelfMember().getVoiceState(),
                        event.getMember().getVoiceState(),
                        event
                );
    }
}
