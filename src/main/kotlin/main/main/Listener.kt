package main.main

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.runBlocking
import main.audiohandlers.RobertifyAudioManager
import main.events.AbstractEventController
import main.utils.GeneralUtils
import main.utils.RobertifyEmbedUtils
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.database.mongodb.cache.BotDBCache
import main.utils.json.guildconfig.GuildConfig
import main.utils.locale.messages.UnbanMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Listener : AbstractEventController() {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)

        internal fun loadSlashCommands(guild: Guild) {
            AbstractSlashCommand.loadAllCommands(guild)
        }

        /**
         * Load slash commands that NEED to be updated in a guild
         * @param guild The guild to load the commands in
         */
        internal fun loadNeededSlashCommands(guild: Guild) {

        }

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

        internal suspend fun rescheduleUnbans(guild: Guild) {
            val guildConfig = GuildConfig(guild)
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
                        val task = Runnable { runBlocking { doUnban(userId, guild, guildConfig) } }
                        scheduler.schedule(task, guildConfig.getTimeUntilUnban(userId), TimeUnit.MILLISECONDS)
                    }
                }
            }
        }

        suspend fun scheduleUnban(guild: Guild, user: User) {
            val guildConfig = GuildConfig(guild)
            val scheduler = Executors.newSingleThreadScheduledExecutor()

            val task = Runnable { runBlocking { doUnban(user.idLong, guild, guildConfig) } }
            scheduler.schedule(task, guildConfig.getTimeUntilUnban(user.idLong), TimeUnit.MILLISECONDS)

        }

        private suspend fun doUnban(userId: Long, guild: Guild, guildConfig: GuildConfig = GuildConfig(guild)) {
            if (!guildConfig.isBannedUser(userId))
                return

            guildConfig.unbanUser(userId)
            sendUnbanMessage(userId, guild)
        }

        private suspend fun sendUnbanMessage(userId: Long, guild: Guild) {
            val user = Robertify.shardManager.retrieveUserById(userId).await()
            val channel = user.openPrivateChannel().await()
            channel.send(
                embeds = listOf(
                    RobertifyEmbedUtils.embedMessage(
                        guild,
                        UnbanMessages.USER_UNBANNED,
                        Pair("{server}", guild.name)
                    ).build()
                )
            ).queue(null) {
                ErrorHandler().handle(ErrorResponse.CANNOT_SEND_TO_USER) {
                    logger.warn("Was not able to send an unban message to ${user.name} (${user.idLong})")
                }
            }
        }
    }

    override fun onGuildReady(event: GuildReadyEvent) {
        logger.info("Guild ${event.guild.name} is ready")
    }

    private val guildJoinListener =
        onEvent<GuildJoinEvent> { event ->
            val guild = event.guild
            GuildConfig(guild).addGuild()
            loadSlashCommands(guild)
            GeneralUtils.setDefaultEmbed(guild)
            logger.info("Joined ${guild.name}")

            updateServerCount()
        }

    private val guildLeaveListener =
        onEvent<GuildLeaveEvent> { event ->
            val guild = event.guild
            GuildConfig(guild).removeGuild()
            RobertifyAudioManager.removeMusicManager(guild)
            logger.info("Left ${guild.name}")

            updateServerCount()
        }

    private fun updateServerCount() {
        val serverCount = Robertify.shardManager.guilds.size

        BotDBCache.instance.setGuildCount(serverCount)
        if (Robertify.topGGAPI != null)
            Robertify.topGGAPI!!.setStats(serverCount)
    }
}