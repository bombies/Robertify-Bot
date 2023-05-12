package main.commands.slashcommands.audio

import dev.minn.jda.ktx.interactions.components.danger
import dev.minn.jda.ktx.interactions.components.success
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import main.audiohandlers.GuildMusicManager
import main.audiohandlers.RobertifyAudioManager
import main.audiohandlers.utils.author
import main.audiohandlers.utils.title
import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.constants.RobertifyPermission
import main.utils.GeneralUtils
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.editEmbed
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.Command
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.json.logs.LogType
import main.utils.json.logs.LogUtilsKt
import main.utils.json.requestchannel.RequestChannelConfig
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.SkipMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.math.ceil

class SkipCommand : AbstractSlashCommand(
    Command(
        name = "skip",
        description = "Skip the song currently being played.",
        options = listOf(
            CommandOption(
                type = OptionType.INTEGER,
                name = "to",
                description = "The ID of the track to skip to",
                required = false
            )
        )
    )
) {

    companion object {
        private val logger by SLF4J
    }

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()

        val guild = event.guild!!
        val selfVoiceState = guild.selfMember.voiceState!!
        val memberVoiceState = event.member!!.voiceState!!

        if (!musicCommandDJCheck(event)) {
            if (!selfVoiceState.inAudioChannel()) {
                event.hook.sendEmbed(guild) {
                    embed(GeneralMessages.VOICE_CHANNEL_NEEDED)
                }.setEphemeral(true)
                    .queue()
                return
            }

            if (selfVoiceState.channel!!.members.size > 2) {
                val embed = handleVoteSkip(event.channel.asGuildMessageChannel(), selfVoiceState, memberVoiceState)
                if (embed != null) {
                    event.hook.sendMessageEmbeds(embed)
                        .queue()
                } else {
                    event.hook.sendEmbed(guild) {
                        embed(SkipMessages.VOTE_SKIP_STARTED)
                    }.queue()
                }
                return
            }
        }

        if (event.options.isEmpty()) {
            event.hook.sendMessageEmbeds(handleSkip(selfVoiceState, memberVoiceState))
                .queue()
        } else {
            val musicManager = RobertifyAudioManager[guild]
            val tracksToSkip = event.getRequiredOption("to").asLong.toInt()
            event.hook.sendMessageEmbeds(handleSkip(event.user, musicManager, tracksToSkip))
                .queue()
        }
    }

    fun handleSkip(selfVoiceState: GuildVoiceState, memberVoiceState: GuildVoiceState): MessageEmbed {
        val guild = selfVoiceState.guild

        val checksEmbed = audioChannelChecks(selfVoiceState, memberVoiceState)
        if (checksEmbed != null)
            return checksEmbed

        val musicManager = RobertifyAudioManager[guild]
        val player = musicManager.player

        if (player.playingTrack == null)
            return RobertifyEmbedUtils.embedMessage(guild, SkipMessages.NOTHING_TO_SKIP)
                .build()

        skip(guild)

        LogUtilsKt(guild).sendLog(
            LogType.TRACK_SKIP, SkipMessages.SKIPPED_LOG,
            Pair("{user}", memberVoiceState.member.asMention)
        )
        return RobertifyEmbedUtils.embedMessage(guild, SkipMessages.SKIPPED).build()
    }

    fun handleSkip(skipper: User, musicManager: GuildMusicManager, id: Int): MessageEmbed {
        val scheduler = musicManager.scheduler
        val queueHandler = scheduler.queueHandler

        if (id > queueHandler.size || id <= 0)
            return RobertifyEmbedUtils.embedMessage(
                musicManager.guild,
                GeneralMessages.INVALID_ARGS
            ).build()

        val player = musicManager.player
        val playingTrack = player.playingTrack!!
        val guild = musicManager.guild
        val currentQueue = queueHandler.contents.toMutableList()
        val songsToRemove = currentQueue.subList(0, id - 1)

        queueHandler.removeAll(songsToRemove)
        queueHandler.pushPastTrack(playingTrack)
        scheduler.nextTrack(playingTrack, true, playingTrack.position)

        RequestChannelConfig(guild).updateMessage()
        SkipCommand().clearVoteSkipInfo(guild)
        return RobertifyEmbedUtils.embedMessage(musicManager.guild, "Skipped to **track #$id**!").build()
    }

    fun handleVoteSkip(
        channel: GuildMessageChannel,
        selfVoiceState: GuildVoiceState,
        memberVoiceState: GuildVoiceState
    ): MessageEmbed? {
        val guild = selfVoiceState.guild

        val checksEmbed = audioChannelChecks(selfVoiceState, memberVoiceState)
        if (checksEmbed != null)
            return checksEmbed

        val musicManager = RobertifyAudioManager[guild]
        val player = musicManager.player

        if (player.playingTrack == null)
            return RobertifyEmbedUtils.embedMessage(guild, SkipMessages.NOTHING_TO_SKIP)
                .build()

        val neededVotes = getNeededVotes(guild)
        channel.sendEmbed(guild) {
            embed(
                SkipMessages.VOTE_SKIP_STARTED_EMBED,
                Pair("{user}", memberVoiceState.member.asMention),
                Pair("{neededVotes}", neededVotes.toString())
            )
        }
            .setActionRow(
                success(
                    id = "voteskip:upvote:${guild.id}",
                    label = "Vote"
                ),
                danger(
                    id = "voteskip:cancel:${guild.id}"
                )
            )
            .queue { msg ->
                val starter = memberVoiceState.member

                LogUtilsKt(guild).sendLog(
                    LogType.TRACK_VOTE_SKIP, SkipMessages.VOTE_SKIP_STARTED_LOG,
                    Pair("{user}", starter.asMention)
                )

                val voteSkipManager = musicManager.voteSkipManager
                voteSkipManager.startedBy = starter.idLong
                voteSkipManager.voteSkipCount = 1
                voteSkipManager.addVoter(starter.idLong)
                voteSkipManager.voteSkipMessage = Pair(channel.idLong, msg.idLong)
            }
        return null
    }

    private fun skip(guild: Guild) {
        val musicManager = RobertifyAudioManager[guild]
        val audioPlayer = musicManager.player
        val scheduler = musicManager.scheduler
        val playingTrack = audioPlayer.playingTrack
        val queueHandler = scheduler.queueHandler

        if (playingTrack == null) {
            logger.warn("Attempting to skip a null track in ${guild.name}!")
            return
        }

        queueHandler.pushPastTrack(playingTrack)

        if (queueHandler.trackRepeating)
            queueHandler.trackRepeating = false

        scheduler.nextTrack(playingTrack, true, audioPlayer.trackPosition)

        RequestChannelConfig(guild).updateMessage()
        clearVoteSkipInfo(guild)
    }

    private fun doVoteSkip(guild: Guild) {
        val voteSkipManager = RobertifyAudioManager[guild].voteSkipManager
        if (!voteSkipManager.isVoteSkipActive)
            throw IllegalStateException("Can't do a vote skip when there's none active!")
        guild.getTextChannelById(voteSkipManager.voteSkipMessage!!.first)
            ?.retrieveMessageById(voteSkipManager.voteSkipMessage!!.second)
            ?.submit()
            ?.whenComplete { msg, err ->
                if (err != null) {
                    logger.error("Unexpected error", err)
                    return@whenComplete
                }

                voteSkipManager.voteSkipMessage = null
                val track = RobertifyAudioManager[guild]
                    .player
                    .playingTrack!!

                runBlocking {
                    launch { skip(guild) }
                }

                msg.editEmbed {
                    embed(
                        SkipMessages.VOTE_SKIPPED,
                        Pair("{title}", track.title),
                        Pair("{author}", track.author)
                    )
                }
                    .setComponents()
                    .queue()

                LogUtilsKt(guild).sendLog(
                    LogType.TRACK_SKIP, SkipMessages.VOTE_SKIPPED_LOG,
                    Pair("{title}", track.title),
                    Pair("{author}", track.author)
                )
            }
    }

    fun clearVoteSkipInfo(guild: Guild) {
        val voteSkipManagerKt = RobertifyAudioManager[guild].voteSkipManager
        voteSkipManagerKt.clearVoters()
        voteSkipManagerKt.startedBy = null
        voteSkipManagerKt.isVoteSkipActive = false
        voteSkipManagerKt.voteSkipCount = 0

        val message = voteSkipManagerKt.voteSkipMessage
        if (message != null) {
            guild.getTextChannelById(message.first)
                ?.retrieveMessageById(message.second)
                ?.queue { msg ->
                    msg.editEmbed { embed(SkipMessages.SKIPPED) }
                        .queue { voteSkipManagerKt.voteSkipMessage = null }
                }
        }
    }

    private fun incrementVoteSkip(guild: Guild) {
        val voteSkipManager = RobertifyAudioManager[guild].voteSkipManager
        check(voteSkipManager.isVoteSkipActive) { "Can't increment vote skips since ${guild.name} (${guild.id}) doesn't have any active vote skips!" }
        voteSkipManager.voteSkipCount++
    }

    private fun decrementVoteSkip(guild: Guild) {
        val voteSkipManager = RobertifyAudioManager[guild].voteSkipManager
        check(voteSkipManager.isVoteSkipActive) { "Can't increment vote skips since ${guild.name} (${guild.id}) doesn't have any active vote skips!" }
        voteSkipManager.voteSkipCount--
    }

    private fun getNeededVotes(guild: Guild): Int {
        val size = guild.selfMember.voiceState!!.channel!!.members.size
        return ceil(size * (50 / 100.0)).toInt()
    }

    override suspend fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (event.button.id?.startsWith("voteskip:") != true)
            return

        val split = event.button.id!!.split(":")
        val voteType = split[1]
        val guildId = split[2]

        val guild = event.guild!!
        if (guild.id != guildId)
            return

        val selfVoiceState = guild.selfMember.voiceState!!
        val memberVoiceState = event.member!!.voiceState!!

        if (!selfVoiceState.inAudioChannel()) {
            event.replyEmbed {
                embed(GeneralMessages.BUTTON_NO_LONGER_VALID)
            }.setEphemeral(true)
                .queue()
            return
        }

        if (!memberVoiceState.inAudioChannel()) {
            event.replyEmbed {
                embed(GeneralMessages.SAME_VOICE_CHANNEL_BUTTON)
            }.setEphemeral(true)
                .queue()
            return
        }

        if (selfVoiceState.channel!!.id != memberVoiceState.channel!!.id) {
            event.replyEmbed {
                embed(GeneralMessages.SAME_VOICE_CHANNEL_BUTTON)
            }.setEphemeral(true)
                .queue()
            return
        }

        val user = memberVoiceState.member.user
        val voteSkipManager = RobertifyAudioManager[guild].voteSkipManager
        when (voteType) {
            "upvote" -> {
                if (voteSkipManager.userAlreadyVoted(user.idLong)) {
                    voteSkipManager.removeVoter(user.idLong)
                    voteSkipManager.voteSkipCount--

                    event.replyEmbed {
                        embed(SkipMessages.SKIP_VOTE_REMOVED)
                    }
                        .setEphemeral(true)
                        .queue()
                    updateVoteSkipMessage(guild)
                } else {
                    if (GeneralUtils.hasPerms(guild, event.member, RobertifyPermission.ROBERTIFY_DJ)) {
                        doVoteSkip(guild)
                        event.replyEmbed {
                            embed(SkipMessages.DJ_SKIPPED)
                        }.setEphemeral(true)
                            .queue()
                        return
                    }

                    voteSkipManager.addVoter(user.idLong)
                    voteSkipManager.voteSkipCount++
                    event.replyEmbed {
                        embed(SkipMessages.SKIP_VOTE_ADDED)
                    }
                        .setEphemeral(true)
                        .queue()
                    updateVoteSkipMessage(guild)
                }
            }

            "cancel" -> {
                if (voteSkipManager.startedBy != user.idLong) {
                    event.replyEmbed {
                        embed(GeneralMessages.NO_PERMS_BUTTON)
                    }.setEphemeral(true)
                        .queue()
                    return
                }

                val message = voteSkipManager.voteSkipMessage!!
                guild.getTextChannelById(message.first)
                    ?.retrieveMessageById(message.second)
                    ?.submit()
                    ?.whenComplete { msg, err ->
                        if (err != null) {
                            logger.error("Unexpected error", err)
                            return@whenComplete
                        }

                        val track = RobertifyAudioManager[guild]
                            .player
                            .playingTrack!!

                        voteSkipManager.voteSkipMessage = null
                        clearVoteSkipInfo(guild)
                        msg.editEmbed {
                            embed(
                                SkipMessages.VOTE_SKIP_CANCELLED,
                                Pair("{title}", track.title),
                                Pair("{author}", track.author)
                            )
                        }.queue()
                    }
            }
        }
    }

    private fun updateVoteSkipMessage(guild: Guild) {
        val voteSkipManager = RobertifyAudioManager[guild].voteSkipManager
        if (!voteSkipManager.isVoteSkipActive)
            return

        val message = voteSkipManager.voteSkipMessage!!
        guild.getTextChannelById(message.first)
            ?.retrieveMessageById(message.second)
            ?.submit()
            ?.thenCompose { msg ->
                val neededvotes = getNeededVotes(guild)
                msg.editEmbed {
                    embed(
                        SkipMessages.VOTE_SKIP_STARTED_EMBED,
                        Pair(
                            "{user}",
                            GeneralUtils.toMention(
                                guild,
                                voteSkipManager.startedBy!!,
                                GeneralUtils.Mentioner.USER
                            )
                        ),
                        Pair("{neededVotes}", neededvotes.toString())
                    )
                }
                    .submit()
            }
            ?.whenComplete { _, err ->
                if (err != null) {
                    logger.error("Unexepected error", err)
                    return@whenComplete
                }
            }
    }

    override val help: String
        get() = "Skips a track or skip to a specific song in the queue"
}