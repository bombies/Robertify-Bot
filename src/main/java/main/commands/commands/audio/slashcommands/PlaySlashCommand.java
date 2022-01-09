package main.commands.commands.audio.slashcommands;

import lombok.SneakyThrows;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.commands.audio.PlayCommand;
import main.main.Listener;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.InteractiveCommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
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
                    .setDescription("Play a song! Links are accepted by either Spotify, YouTube, SoundCloud, etc...")
                    .addOption(CommandOption.of(OptionType.STRING, "track", "The name/link of a track", true))
                    .setPermissionCheck(djPredicate)
                    .build()
                .build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(new PlayCommand().getName())) return;

        event.deferReply().queue();

        final var guild = event.getGuild();

        if (!getCommand().getCommand().permissionCheck(event)) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You need to be a DJ to run this command!").build())
                    .queue();
            return;
        }

        EmbedBuilder eb;
        final TextChannel channel = event.getTextChannel();

        if (!new GuildConfig().announcementChannelIsSet(guild.getIdLong())) {
            if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong())) {
                if (channel.getIdLong() == new DedicatedChannelConfig().getChannelID(guild.getIdLong())) {
                    event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You cannot run this command in this channel " +
                                    "without first having an announcement channel set!").build())
                            .setEphemeral(false)
                            .queue();
                    return;
                }
            }
        }

        Listener.checkIfAnnouncementChannelIsSet(guild, channel);

        final Member member = event.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();

        if (!memberVoiceState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You need to be in a voice channel for this to work");
            event.getHook().sendMessageEmbeds(eb.build()).setEphemeral(true).queue();
            return;
        }

        if (selfVoiceState.inVoiceChannel() && !memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!")
                            .build())
                    .queue();
            return;
        }

        String link = event.getOption("track").getAsString();

        if (!GeneralUtils.isUrl(link)) {
            link = "ytsearch:" + link;
        }

        String finalLink = link;
        event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Adding to queue...").build())
                        .setEphemeral(false)
                                .queue(msg -> RobertifyAudioManager.getInstance()
                                        .loadAndPlay(
                                                finalLink,
                                                event.getGuild().getSelfMember().getVoiceState(),
                                                event.getMember().getVoiceState(),
                                                msg,
                                                event
                                        ));


    }
}
