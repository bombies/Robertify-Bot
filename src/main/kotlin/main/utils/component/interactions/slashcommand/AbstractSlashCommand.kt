package main.utils.component.interactions.slashcommand

import com.influxdb.exceptions.InfluxException
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.CoroutineEventListener
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.events.onCommand
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import main.audiohandlers.RobertifyAudioManager
import main.commands.slashcommands.SlashCommandManager
import main.constants.BotConstants
import main.constants.RobertifyPermission
import main.constants.Toggle
import main.main.Config
import main.main.Robertify
import main.utils.GeneralUtils
import main.utils.GeneralUtils.asString
import main.utils.GeneralUtils.hasPermissions
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.GenericInteraction
import main.utils.component.interactions.slashcommand.models.MutableSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.database.influxdb.databases.commands.CommandInfluxDatabase
import main.utils.database.mongodb.cache.BotDBCache
import main.utils.json.guildconfig.GuildConfig
import main.utils.json.requestchannel.RequestChannelConfig
import main.utils.json.restrictedchannels.RestrictedChannelsConfig
import main.utils.json.toggles.TogglesConfig
import main.utils.locale.LocaleManager
import main.utils.locale.messages.GeneralMessages
import main.utils.managers.RandomMessageManager
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

abstract class AbstractSlashCommand protected constructor(val info: SlashCommand) : GenericInteraction {

    constructor(block: MutableSlashCommand.() -> Unit) : this(handleConstructorBlock(block))

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)

        private fun handleConstructorBlock(block: MutableSlashCommand.() -> Unit): SlashCommand {
            val mutableSlashCommand = MutableSlashCommand()
            block(mutableSlashCommand)

            require(mutableSlashCommand.name.isNotEmpty()) { "The name of the slash command must be provided!" }
            require(mutableSlashCommand.description.isNotEmpty()) { "The name of the description command must be provided!" }

            return mutableSlashCommand.toImmutable()
        }

        fun loadAllCommands() {
            val slashCommandManager = SlashCommandManager
            val commands = slashCommandManager.globalCommands

            Robertify.shardManager.shards.forEach { shard ->
                val commandListUpdateAction = shard.updateCommands()

                // TODO: Integrate context commands

                commands.forEach { commandListUpdateAction.addCommands(it.info.getCommandData()) }
                commandListUpdateAction.queueAfter(1, TimeUnit.SECONDS)
            }
        }

        fun loadAllCommands(guild: Guild) {
            val slashCommandManager = SlashCommandManager
            val commands = slashCommandManager.guildCommands
            val devCommands = slashCommandManager.devCommands

            val commandListUpdateAction = guild.updateCommands()

            // TODO: Integrate context commands

            commands.forEach { commandListUpdateAction.addCommands(it.info.getCommandData()) }

            if (guild.ownerIdLong == Config.OWNER_ID)
                devCommands.forEach { commandListUpdateAction.addCommands(it.info.getCommandData()) }

            commandListUpdateAction.queueAfter(1, TimeUnit.SECONDS, null,
                ErrorHandler()
                    .handle(ErrorResponse.fromCode(30034)) {
                        guild.retrieveOwner().queue { owner ->
                            owner.user.openPrivateChannel().queue { channel ->
                                channel.sendMessageEmbeds(
                                    RobertifyEmbedUtils.embedMessage(
                                        "Hey, I could not create some slash commands in **${guild.name}**" +
                                                " due to being re-invited too many times. Try inviting me again tomorrow to fix this issue."
                                    ).build()
                                )
                                    .queue(null, ErrorHandler().ignore(ErrorResponse.CANNOT_SEND_TO_USER))
                            }
                        }
                    }
                    .handle(ErrorResponse.MISSING_ACCESS) {
                        logger.warn("I wasn't able update to update commands in ${guild.name}!")
                    }
            )
        }


        fun upsertCommand(command: AbstractSlashCommand) {
            Robertify.shardManager.shards.forEach { shard ->
                shard.upsertCommand(command.info.getCommandData()).queue()
            }
        }

        fun unloadAllCommands(guild: Guild) {
            if (guild.ownerIdLong != Config.OWNER_ID)
                guild.updateCommands().addCommands().queue()
            else
                guild.updateCommands()
                    .addCommands(
                        SlashCommandManager
                            .devCommands
                            .map { it.info.getCommandData() }
                    )
                    .queue(null, ErrorHandler()
                        .handle(ErrorResponse.MISSING_ACCESS) {
                            logger.error("I didn't have enough permission to unload guild commands from ${guild.name}")
                        }
                    )
        }

        /**
         * Executes the necessary checks for audio commands.
         * @param memberVoiceState The voice state of the user executing the command.
         * @param selfVoiceState The voice state of the bot.
         * @param selfChannelNeeded If the bot needs to be in a voice channel for the command to be executed.
         * @param songMustBePlaying If the bot needs to be playing a song for the command to be executed.
         * @return An embed of the error message, null if all the checks passed.
         */
        fun audioChannelChecks(
            memberVoiceState: GuildVoiceState,
            selfVoiceState: GuildVoiceState,
            selfChannelNeeded: Boolean = true,
            songMustBePlaying: Boolean = false,
        ): MessageEmbed? {
            val guild = selfVoiceState.guild

            if (selfChannelNeeded && !selfVoiceState.inAudioChannel())
                return RobertifyEmbedUtils.embedMessage(
                    guild,
                    GeneralMessages.NO_VOICE_CHANNEL
                )
                    .build()

            if (!memberVoiceState.inAudioChannel())
                return RobertifyEmbedUtils.embedMessage(
                    guild,
                    GeneralMessages.USER_VOICE_CHANNEL_NEEDED
                ).build()

            if (selfChannelNeeded && memberVoiceState.channel!!.id != selfVoiceState.channel!!.id)
                return RobertifyEmbedUtils.embedMessage(
                    guild, GeneralMessages.SAME_VOICE_CHANNEL_LOC,
                    Pair("{channel}", selfVoiceState.channel!!.asMention)
                ).build()
            else if (!selfChannelNeeded && selfVoiceState.inAudioChannel() && (memberVoiceState.channel!!.id != selfVoiceState.channel!!.id))
                return RobertifyEmbedUtils.embedMessage(
                    guild,
                    GeneralMessages.SAME_VOICE_CHANNEL
                ).build()

            val player = RobertifyAudioManager[guild].player
            val playingTrack = player?.track
            if (songMustBePlaying && playingTrack == null)
                return RobertifyEmbedUtils.embedMessage(
                    guild,
                    GeneralMessages.NOTHING_PLAYING
                ).build()

            return null
        }
    }

    suspend fun register(shardManager: ShardManager) {
        shardManager.onCommand(this.info.name) { event ->
            if (event !is SlashCommandInteractionEvent)
                return@onCommand

            val checks = checks(event)
            if (!checks)
                return@onCommand

            handle(event)

            if (!SlashCommandManager.isDevCommand(this@AbstractSlashCommand)) {
                try {
                    CommandInfluxDatabase.recordCommand(
                        guild = event.guild,
                        command = this@AbstractSlashCommand,
                        executor = event.user
                    )
                } catch (e: InfluxException) {
                    logger.warn("Could not record the ${this@AbstractSlashCommand.info.name} command in InfluxDB. Reason: ${e.message ?: "Unknown"}")
                }
            }
        }

        onEvent<ButtonInteractionEvent>(shardManager) {
            onButtonInteraction(it)
        }

        onEvent<StringSelectInteractionEvent>(shardManager) {
            onStringSelectInteraction(it)
        }

        onEvent<CommandAutoCompleteInteractionEvent>(shardManager) { event ->
            if (event.name != info.name)
                return@onEvent
            onCommandAutoCompleteInteraction(event)
        }

        onEvent<ModalInteractionEvent>(shardManager) {
            onModalInteraction(it)
        }

    }

    private inline fun <reified T : GenericEvent> onEvent(
        shardManager: ShardManager,
        crossinline handler: suspend CoroutineEventListener.(event: T) -> Unit
    ) =
        shardManager.listener<T> { handler(it) }

    abstract suspend fun handle(event: SlashCommandInteractionEvent)

    open suspend fun onButtonInteraction(event: ButtonInteractionEvent) {}

    open suspend fun onStringSelectInteraction(event: StringSelectInteractionEvent) {}

    open suspend fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {}

    open suspend fun onModalInteraction(event: ModalInteractionEvent) {}

    open val help = ""

    fun loadCommand(guild: Guild) {
        if (!info.isGuild && !info.developerOnly)
            return

        if (info.developerOnly && guild.ownerIdLong != Config.OWNER_ID)
            return

        logger.debug("Loading command \"${info.name}\" in ${guild.name}")

        guild.upsertCommand(info.getCommandData())
            .queueAfter(1, TimeUnit.SECONDS, null,
                ErrorHandler()
                    .handle(ErrorResponse.MISSING_ACCESS) {
                        logger.warn("I couldn't load the ${info.name} command in ${guild.name} because I am missing permission!")
                    }
            )
    }

    fun loadCommand() {
        if (info.isGuild)
            return
        if (info.developerOnly)
            return

        Robertify.shardManager.shards.forEach { shard ->
            shard.upsertCommand(info.getCommandData()).queue()
        }
    }

    fun unload(guild: Guild) {
        guild.retrieveCommands().queue { commands ->
            val matchedCommand = commands.find { it.name == info.name } ?: return@queue
            guild.deleteCommandById(matchedCommand.idLong).queue()
        }
    }

    fun unload() {
        Robertify.shardManager.shards.forEach { shard ->
            shard.retrieveCommands().queue { commands ->
                val matchedCommand = commands.find { it.name == info.name } ?: return@queue
                shard.deleteCommandById(matchedCommand.idLong).queue()
            }
        }
    }

    fun reload() {
        if (info.isGuild)
            return
        upsertCommand(this)
    }

    protected fun sendRandomMessage(event: SlashCommandInteractionEvent) {
        if (SlashCommandManager.isMusicCommand(this) && event.channel.type.isMessage)
            RandomMessageManager().randomlySendMessage(event.channel as GuildMessageChannel)
    }

    protected fun checks(event: SlashCommandInteractionEvent): Boolean {
        if (!nameCheck(event)) return false
        if (!guildCheck(event)) return false
        if (!event.isFromGuild) return true
        if (!botEmbedCheck(event)) return false
        if (!banCheck(event)) return false
        if (!restrictedChannelCheck(event)) return false
        if (!botPermsCheck(event)) return false
        if (!premiumBotCheck(event)) return false

        if (SlashCommandManager.isMusicCommand(this)) {
            val botDB = BotDBCache.instance
            val latestAlert = botDB.latestAlert.first
            val user = event.user
            if (!botDB.userHasViewedAlert(user.idLong) && latestAlert.isNotEmpty() && latestAlert.isNotBlank()
                && SlashCommandManager.isMusicCommand(this)
            ) {
                event.channel.asGuildMessageChannel().sendMessageEmbeds(
                    RobertifyEmbedUtils.embedMessage(
                        event.guild,
                        GeneralMessages.UNREAD_ALERT_MENTION,
                        Pair("{user}", user.asMention)
                    ).build()
                ).queue { msg ->
                    val dedicatedChannelConfig = RequestChannelConfig(msg.guild)
                    if (dedicatedChannelConfig.isChannelSet()) if (dedicatedChannelConfig.getChannelId() == msg.channel
                            .idLong
                    ) msg.delete().queueAfter(
                        10, TimeUnit.SECONDS, null,
                        ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)
                    )
                }
            }
        }

        if (!adminCheck(event)) return false
        if (!djCheck(event)) return false
        if (!predicateCheck(event)) return false

        if (SlashCommandManager.isMusicCommand(this)) {
            val scheduler = RobertifyAudioManager
                .getMusicManager(event.guild!!)
                .scheduler
            scheduler.announcementChannel = event.guildChannel
        }

        return true
    }

    /**
     * Checks if the name of the command in the event passed is the same name as this command object
     * @param event
     * @return True if the names are the same, false if otherwise.
     */
    protected open fun nameCheck(event: SlashCommandInteractionEvent): Boolean =
        info.name == event.name


    protected open fun guildCheck(event: SlashCommandInteractionEvent): Boolean {
        if (info.isGuild) return true
        if (!event.isFromGuild && info.guildUseOnly) {
            event.replyEmbeds(
                RobertifyEmbedUtils.embedMessage(GeneralMessages.GUILD_COMMAND_ONLY).build()
            )
                .queue()
            return false
        }
        return true
    }

    /***
     * Checks if the user who is attempting to execute the command is a banned user
     * @param event
     * @return True if the user is banned, false if otherwise.
     */
    protected open fun banCheck(event: SlashCommandInteractionEvent): Boolean {
        val guild = event.guild ?: return true
        if (!GuildConfig(guild).isBannedUser(event.user.idLong)) return true
        event.replyEmbeds(
            RobertifyEmbedUtils.embedMessage(
                guild,
                GeneralMessages.BANNED_FROM_COMMANDS
            ).build()
        )
            .setEphemeral(true)
            .queue()
        return false
    }

    /**
     * Checks if the command is a possible DJ command.
     * If the command is a DJ command it then checks if
     * the author of the command has permission to run the command
     * @param event The slash command event.
     * @return True - If the command isn't a DJ command or the user is a DJ
     * False - If the command is a DJ command and the user isn't a DJ.
     */
    protected open fun musicCommandDJCheck(event: SlashCommandInteractionEvent): Boolean {
        return predicateCheck(event)
    }

    /**
     * Checks if the command is a premium command.
     * If the command is a premium command it then
     * checks if the author of the command is a premium user.
     * @param event The slash command event.
     * @return True - If the command isn't a premium command or the user is a premium user
     * False - If the command is a premium command and the user isn't a premium user
     */
    protected open fun premiumCheck(event: SlashCommandInteractionEvent): Boolean {
        if (!event.isFromGuild) return true
        if (!info.isPremium) return true
        val user = event.user
        return !(!GeneralUtils.checkPremium(event.guild!!, event) && user.idLong != 276778018440085505L)
    }

    protected open fun premiumBotCheck(event: SlashCommandInteractionEvent): Boolean {
        if (!Config.PREMIUM_BOT) return true
        val guild = event.guild ?: return true
        if (!GuildConfig(guild).isPremium()) {
            event.replyEmbeds(
                RobertifyEmbedUtils.embedMessageWithTitle(
                    guild,
                    GeneralMessages.PREMIUM_EMBED_TITLE,
                    GeneralMessages.PREMIUM_INSTANCE_NEEDED
                ).build()
            )
                .addActionRow(
                    Button.link(
                        "https://robertify.me/premium",
                        LocaleManager[guild]
                            .getMessage(GeneralMessages.PREMIUM_UPGRADE_BUTTON)
                    )
                )
                .queue()
            return false
        }
        return true
    }

    /***
     * Checks if the user executing the command is a DJ or not if the command is DJ-only.
     * @param event
     * @return If the command is a DJ-only command, true will be returned if the user
     * attempting to execute the command is a DJ. If the command is not DJ-only,
     * true will be returned.
     * False will be returned if and only if the command is DJ-only and the user is not a DJ.
     */
    protected open fun djCheck(event: SlashCommandInteractionEvent): Boolean {
        if (!event.isFromGuild) return true
        val guild = event.guild!!
        val toggles = TogglesConfig(guild)

        if ((info.djOnly || toggles.getDJToggle(this))
            && !GeneralUtils.hasPerms(guild, event.member, RobertifyPermission.ROBERTIFY_DJ)
            && !GeneralUtils.isDeveloper(event.user.idLong)
        ) {
            event.replyEmbeds(
                RobertifyEmbedUtils.embedMessage(
                    guild,
                    BotConstants.getInsufficientPermsMessage(guild, RobertifyPermission.ROBERTIFY_DJ)
                ).build()
            )
                .setEphemeral(true)
                .queue()
            return false
        }
        return true
    }

    /***
     * Checks if the user executing the command is an admin or not if the command is admin-only.
     * @param event
     * @return If the command is an admin-only command, true will be returned if the user
     * attempting to execute the command is an admin. If the command is not admin-only,
     * true will be returned.
     * False will be returned if and only if the command is admin-only and the user is not an admin.
     */
    protected open fun adminCheck(event: SlashCommandInteractionEvent): Boolean {
        if (!event.isFromGuild) return true
        val guild = event.guild!!
        if (info.adminOnly
            && !GeneralUtils.hasPerms(guild, event.member, RobertifyPermission.ROBERTIFY_ADMIN)
            && !GeneralUtils.isDeveloper(event.user.idLong)
        ) {
            event.replyEmbeds(
                RobertifyEmbedUtils.embedMessage(
                    guild,
                    BotConstants.getInsufficientPermsMessage(guild, RobertifyPermission.ROBERTIFY_ADMIN)
                ).build()
            )
                .setEphemeral(true)
                .queue()
            return false
        }
        return true
    }

    protected open fun predicateCheck(event: SlashCommandInteractionEvent): Boolean {
        return if (info.checkPermission == null && info.requiredPermissions.isNotEmpty()) {
            if (!event.isFromGuild)
                true
            else if (!event.member!!.hasPermissions(*info.requiredPermissions.toTypedArray())) {
                event.replyEmbed {
                    embed(
                        BotConstants.getInsufficientPermsMessage(
                            event.guild,
                            *info.requiredPermissions.toTypedArray()
                        )
                    )
                }.setEphemeral(true)
                    .queue()
                false
            } else true
        } else
            if (GeneralUtils.isDeveloper(event.id)) true else info.checkPermission?.invoke(event) ?: true
    }

    protected open fun botPermsCheck(event: SlashCommandInteractionEvent): Boolean {
        if (info.botRequiredPermissions.isEmpty()) return true
        val guild = event.guild ?: return true
        val self = guild.selfMember
        if (!self.hasPermission(info.botRequiredPermissions)) {
            event.replyEmbeds(
                RobertifyEmbedUtils.embedMessage(
                    guild,
                    GeneralMessages.SELF_INSUFFICIENT_PERMS_ARGS,
                    Pair(
                        "{permissions}",
                        info.botRequiredPermissions.asString()
                    )
                ).build()
            )
                .queue()
            return false
        }
        return true
    }

    protected open fun botEmbedCheck(event: SlashCommandInteractionEvent): Boolean {
        val guild = event.guild ?: return true
        if (!guild.selfMember.hasPermission(event.guildChannel, Permission.MESSAGE_EMBED_LINKS)) {
            event.reply(
                LocaleManager[guild]
                    .getMessage(GeneralMessages.NO_EMBED_PERMS)
            )
                .queue()
            return false
        }
        return true
    }

    protected open fun restrictedChannelCheck(event: SlashCommandInteractionEvent): Boolean {
        val guild = event.guild ?: return true
        val togglesConfig = TogglesConfig(guild)
        val config = RestrictedChannelsConfig(guild)
        if (!togglesConfig.getToggle(Toggle.RESTRICTED_TEXT_CHANNELS)) return true
        if (!config.isRestrictedChannel(event.channel.idLong, RestrictedChannelsConfig.ChannelType.TEXT_CHANNEL)
            && !GeneralUtils.hasPerms(guild, event.member, RobertifyPermission.ROBERTIFY_ADMIN)
            && !GeneralUtils.isDeveloper(event.user.idLong)
        ) {
            event.replyEmbeds(
                RobertifyEmbedUtils.embedMessage(
                    guild, GeneralMessages.CANT_BE_USED_IN_CHANNEL_ARGS,
                    Pair(
                        "{channels}", GeneralUtils.listOfIDsToMentions(
                            guild,
                            config.getRestrictedChannels(RestrictedChannelsConfig.ChannelType.TEXT_CHANNEL),
                            GeneralUtils.Mentioner.CHANNEL
                        )
                    )
                ).build()
            )
                .setEphemeral(true)
                .queue()
            return false
        }
        return true
    }

    protected open fun devCheck(event: SlashCommandInteractionEvent): Boolean {
        if (!nameCheck(event)) return false
        if (!info.developerOnly) return true
        if (!BotDBCache.instance.isDeveloper(event.user.idLong)) {
            event.replyEmbeds(
                RobertifyEmbedUtils.embedMessage(
                    event.guild,
                    GeneralMessages.INSUFFICIENT_PERMS_NO_ARGS
                ).build()
            )
                .setEphemeral(true)
                .queue()
            return false
        }
        return true
    }

}