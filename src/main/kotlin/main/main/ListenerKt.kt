package main.main

import dev.minn.jda.ktx.messages.send
import main.audiohandlers.RobertifyAudioManagerKt
import main.events.AbstractEventControllerKt
import main.utils.GeneralUtilsKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.database.mongodb.cache.BotDBCacheKt
import main.utils.json.guildconfig.GuildConfigKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import main.utils.locale.messages.UnbanMessages
import main.utils.resume.GuildResumeManagerKt
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ListenerKt : AbstractEventControllerKt() {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)

        internal fun loadSlashCommands(guild: Guild) {
            AbstractSlashCommandKt.loadAllCommands(guild)
        }

        /**
         * Load slash commands that NEED to be updated in a guild
         * @param g The guild to load the commands in
         */
        internal fun loadNeededSlashCommands(guild: Guild) {}

        internal fun unloadCommands(guild: Guild, vararg commandNames: String) {
            if (commandNames.isEmpty())
                return

            val safeCommandNames = commandNames.map { it.lowercase() }
            guild.retrieveCommands().queue { commands ->
                commands
                    .filter { safeCommandNames.contains(it.name.lowercase()) }
                    .forEach { command -> guild.deleteCommandById(command.idLong).queue() }
            }
        }

        internal fun rescheduleUnbans(guild: Guild) {
            val guildConfig = GuildConfigKt(guild)
            val banMap = guildConfig.getBannedUsersWithUnbanTimes()

            banMap.forEach { (userId, unbanTime) ->

                if (unbanTime != -1L) {
                    if (unbanTime - System.currentTimeMillis() <= 0) {
                        try {
                            doUnban(userId, guild, guildConfig)
                        } catch (e: IllegalArgumentException) {
                            banMap.remove(userId)
                        }
                    } else {
                        val scheduler = Executors.newSingleThreadScheduledExecutor()
                        val task = Runnable { doUnban(userId, guild, guildConfig) }
                        scheduler.schedule(task, guildConfig.getTimeUntilUnban(userId), TimeUnit.MILLISECONDS)
                    }
                }
            }
        }

        fun scheduleUnban(guild: Guild, user: User) {
            val guildConfig = GuildConfigKt(guild)
            val scheduler = Executors.newSingleThreadScheduledExecutor()

            val task = Runnable { doUnban(user.idLong, guild, guildConfig) }
            scheduler.schedule(task, guildConfig.getTimeUntilUnban(user.idLong), TimeUnit.MILLISECONDS)

        }

        private fun doUnban(userId: Long, guild: Guild, guildConfig: GuildConfigKt = GuildConfigKt(guild)) {
            if (!guildConfig.isBannedUser(userId))
                return

            guildConfig.unbanUser(userId)
            sendUnbanMessage(userId, guild)
        }

        private fun sendUnbanMessage(userId: Long, guild: Guild) {
            RobertifyKt.shardManager.retrieveUserById(userId).queue { user ->
                user.openPrivateChannel().queue { channel ->
                    channel.send(
                        embeds = listOf(
                            RobertifyEmbedUtilsKt.embedMessage(
                                guild,
                                UnbanMessages.USER_UNBANNED,
                                Pair("{server}", guild.name)
                            ).build()
                        )
                    ).queue(null) {
                        ErrorHandler().handle(ErrorResponse.CANNOT_SEND_TO_USER) {
                            logger.warn("Was not able to send an unban message to ${user.asTag} (${user.idLong})")
                        }
                    }
                }
            }
        }
    }

    override fun eventHandlerInvokers() {
        guildJoinListener()
        guildLeaveListener()
    }

    private fun guildJoinListener() =
        onEvent<GuildLeaveEvent> { event ->
            val guild = event.guild
            GuildConfigKt(guild).addGuild()
            loadSlashCommands(guild)
            GeneralUtilsKt.setDefaultEmbed(guild)
            logger.info("Joined ${guild.name}")

            updateServerCount()
        }

    private fun guildLeaveListener() =
        onEvent<GuildLeaveEvent> { event ->
            val guild = event.guild
            GuildConfigKt(guild).removeGuild()
            RobertifyAudioManagerKt.removeMusicManager(guild)
            logger.info("Left ${guild.name}")

            updateServerCount()
        }

    private fun updateServerCount() {
        val serverCount = shardManager.guilds.size

        BotDBCacheKt.instance.setGuildCount(serverCount)
        if (RobertifyKt.topGGAPI != null)
            RobertifyKt.topGGAPI!!.setStats(serverCount)
    }
}