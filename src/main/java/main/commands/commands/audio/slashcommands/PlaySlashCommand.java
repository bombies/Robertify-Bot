package main.commands.commands.audio.slashcommands;

import lombok.SneakyThrows;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.commands.audio.PlayCommand;
import main.utils.GeneralUtils;
import main.utils.component.InteractiveCommand;
import main.utils.database.sqlite3.BotDB;
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
        setInteractionCommand(getCommand());
        upsertCommand();
    }

    @Override @SneakyThrows
    public void initCommand(Guild g) {
        setInteractionCommand(getCommand());
        upsertCommand(g);
    }

    @SneakyThrows
    private InteractionCommand getCommand() {
        return InteractiveCommand.InteractionCommand.create()
                .buildCommand()
                    .setName(new PlayCommand().getName())
                    .setDescription("Play a song! Links are accepted by either Spotify or YouTube")
                    .addOption(CommandOption.of(OptionType.STRING, "track", "The name/link of a track", true))
                    .setPermissionCheck(djPredicate)
                    .build()
                .build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(new PlayCommand().getName())) return;

        event.deferReply().queue();

        if (!getCommand().getCommand().permissionCheck(event)) {
            event.getHook().sendMessageEmbeds(EmbedUtils.embedMessage("You need to be a DJ to run this command!").build())
                    .queue();
            return;
        }

        EmbedBuilder eb;

        BotDB botUtils = new BotDB();
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
            event.getHook().sendMessageEmbeds(eb.build()).setEphemeral(false).queue();
            return;
        }

        botUtils.createConnection();

        String link = event.getOption("track").getAsString();

        if (!GeneralUtils.isUrl(link)) {
            link = "ytsearch:" + link;
        }

        String finalLink = link;
        event.getHook().sendMessageEmbeds(EmbedUtils.embedMessage("Adding to queue...").build())
                        .setEphemeral(false)
                                .queue(msg -> {
                                    RobertifyAudioManager.getInstance()
                                            .loadAndPlay(
                                                    finalLink,
                                                    event.getGuild().getSelfMember().getVoiceState(),
                                                    event.getMember().getVoiceState(),
                                                    msg,
                                                    event
                                            );
                                });


    }
}
