package main.utils.component.interactions.slashcommand

import dev.minn.jda.ktx.events.CoroutineEventListener
import dev.minn.jda.ktx.events.listener
import main.audiohandlers.RobertifyAudioManagerKt
import main.commands.slashcommands.SlashCommandManagerKt
import main.constants.BotConstantsKt
import main.constants.PermissionKt
import main.constants.ToggleKt
import main.main.ConfigKt
import main.main.RobertifyKt
import main.utils.GeneralUtilsKt
import main.utils.GeneralUtilsKt.Companion.asString
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.component.AbstractInteractionKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.database.mongodb.cache.BotDBCacheKt
import main.utils.json.guildconfig.GuildConfigKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import main.utils.json.restrictedchannels.RestrictedChannelsConfigKt
import main.utils.json.toggles.TogglesConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import main.utils.managers.RandomMessageManagerKt
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.interactions.components.ComponentInteraction
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

abstract class AbstractSlashCommandKt protected constructor(val info: CommandKt) : AbstractInteractionKt {
    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)

        fun loadAllCommands() {
            val slashCommandManager = SlashCommandManagerKt
            val commands = slashCommandManager.globalCommands

            RobertifyKt.shardManager.shards.forEach { shard ->
                val commandListUpdateAction = shard.updateCommands()

                // TODO: Integrate context commands

                commands.forEach { commandListUpdateAction.addCommands(it.info.getCommandData()) }
                commandListUpdateAction.queueAfter(1, TimeUnit.SECONDS)
            }
        }

        fun loadAllCommands(guild: Guild) {
            val slashCommandManager = SlashCommandManagerKt
            val commands = slashCommandManager.guildCommands
            val devCommands = slashCommandManager.devCommands

            val commandListUpdateAction = guild.updateCommands()

            // TODO: Integrate context commands

            commands.forEach { commandListUpdateAction.addCommands(it.info.getCommandData()) }

            if (guild.ownerIdLong == ConfigKt.OWNER_ID)
                devCommands.forEach { commandListUpdateAction.addCommands(it.info.getCommandData()) }

            commandListUpdateAction.queueAfter(1, TimeUnit.SECONDS, null,
                ErrorHandler()
                    .handle(ErrorResponse.fromCode(30034)) {
                        guild.retrieveOwner().queue { owner ->
                            owner.user.openPrivateChannel().queue { channel ->
                                channel.sendMessageEmbeds(
                                    RobertifyEmbedUtilsKt.embedMessage(
                                        guild,
                                        "Hey, I could not create some slash commands in **${guild.name}**" +
                                                " due to being re-invited too many times. Try inviting me again tomorrow to fix this issue."
                                    ).build()
                                )
                                    .queue(null, ErrorHandler().ignore(ErrorResponse.CANNOT_SEND_TO_USER))
                            }
                        }
                    }
                    .handle(ErrorResponse.MISSING_ACCESS) {
                        logger.warn("I wasn't update to update commands in ${guild.name}!")
                    }
            )
        }


        fun upsertCommand(command: AbstractSlashCommandKt) {
            RobertifyKt.shardManager.shards.forEach { shard ->
                shard.upsertCommand(command.info.getCommandData()).queue()
            }
        }

        fun unloadAllCommands(guild: Guild) {
            if (guild.ownerIdLong != ConfigKt.OWNER_ID)
                guild.updateCommands().addCommands().queue()
            else
                guild.updateCommands()
                    .addCommands(
                        SlashCommandManagerKt
                            .devCommands
                            .map { it.info.getCommandData() }
                    )
                    .queue(null, ErrorHandler()
                        .handle(ErrorResponse.MISSING_ACCESS) {
                            logger.error("I didn't have enough permission to unload guild commands from ${guild.name}")
                        }
                    )
        }

        fun MessageEmbed?.isNull(): Boolean = this == null
        fun MessageEmbed?.isNotNull(): Boolean = this != null
    }

    fun register(shardManager: ShardManager) {
        onEvent<SlashCommandInteractionEvent>(shardManager) { event ->
            if (!checks(event))
                return@onEvent
            handle(event)
        }

        onEvent<ButtonInteractionEvent>(shardManager) {
            onButtonInteraction(it)
        }

        onEvent<StringSelectInteractionEvent>(shardManager) {
            onStringSelectInteraction(it)
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

    open val help = ""

    fun loadCommand(guild: Guild) {
        if (!info.isGuild && !info.isPrivate)
            return

        if (info.isPrivate && guild.ownerIdLong != ConfigKt.OWNER_ID)
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
        if (info.isPrivate)
            return

        RobertifyKt.shardManager.shards.forEach { shard ->
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
        RobertifyKt.shardManager.shards.forEach { shard ->
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
        if (SlashCommandManagerKt.isMusicCommand(this) && event.channel.type.isMessage)
            RandomMessageManagerKt().randomlySendMessage(event.channel as GuildMessageChannel)
    }

    protected fun audioChannelChecks(
        memberVoiceState: GuildVoiceState,
        selfVoiceState: GuildVoiceState,
        selfChannelNeeded: Boolean = true,
        songMustBePlaying: Boolean = false,
    ): MessageEmbed? {
        val guild = selfVoiceState.guild

        if (selfChannelNeeded && !selfVoiceState.inAudioChannel())
            return RobertifyEmbedUtilsKt.embedMessage(guild, RobertifyLocaleMessageKt.GeneralMessages.NOTHING_PLAYING)
                .build()

        if (!memberVoiceState.inAudioChannel())
            return RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.GeneralMessages.USER_VOICE_CHANNEL_NEEDED
            ).build()

        if (selfChannelNeeded && memberVoiceState.channel!!.id != selfVoiceState.channel!!.id)
            return RobertifyEmbedUtilsKt.embedMessage(
                guild, RobertifyLocaleMessageKt.GeneralMessages.SAME_VOICE_CHANNEL_LOC,
                Pair("{channel}", selfVoiceState.channel!!.asMention)
            ).build()
        else if (!selfChannelNeeded && selfVoiceState.inAudioChannel() && (memberVoiceState.channel!!.id != selfVoiceState.channel!!.id))
            return RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.GeneralMessages.SAME_VOICE_CHANNEL
            ).build()

        val playingTrack = RobertifyAudioManagerKt.getMusicManager(guild)
            .player
            .playingTrack
        if (songMustBePlaying && playingTrack == null)
            return RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.GeneralMessages.NOTHING_PLAYING
            ).build()

        return null
    }

    protected fun checks(event: SlashCommandInteractionEvent): Boolean {
        if (!nameCheck(event)) return false
        if (!guildCheck(event)) return false
        if (!botEmbedCheck(event)) return false
        if (!banCheck(event)) return false
        if (!restrictedChannelCheck(event)) return false
        if (!botPermsCheck(event)) return false
        if (!premiumBotCheck(event)) return false

        if (SlashCommandManagerKt.isMusicCommand(this)) {
            val botDB = BotDBCacheKt.instance
            val latestAlert = botDB.getLatestAlert()?.left
            val user = event.user
            if (!botDB.userHasViewedAlert(user.idLong) && !latestAlert.isNullOrEmpty() && latestAlert.isNotBlank()
                && SlashCommandManagerKt.isMusicCommand(this)
            ) event.channel.asGuildMessageChannel().sendMessageEmbeds(
                RobertifyEmbedUtilsKt.embedMessage(
                    event.guild,
                    RobertifyLocaleMessageKt.GeneralMessages.UNREAD_ALERT_MENTION,
                    Pair("{user}", user.asMention)
                ).build()
            )
                .queue(
                    { msg: Message ->
                        val dedicatedChannelConfig = RequestChannelConfigKt(msg.guild)
                        if (dedicatedChannelConfig.isChannelSet()) if (dedicatedChannelConfig.getChannelID() == msg.channel
                                .idLong
                        ) msg.delete().queueAfter(
                            10, TimeUnit.SECONDS, null,
                            ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)
                        )
                    }, ErrorHandler().handle(
                        ErrorResponse.MISSING_PERMISSIONS
                    ) { e: ErrorResponseException ->
                        if (!e.message!!.contains(
                                "MESSAGE_SEND"
                            )
                        ) logger.error(
                            "Unexpected error when attempting to send an unread alert message",
                            e.cause
                        )
                    })
        }

        if (!adminCheck(event)) return false
        if (!djCheck(event)) return false

        if (SlashCommandManagerKt.isMusicCommand(this) && event.isFromGuild) {
            val scheduler = RobertifyAudioManagerKt
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
                RobertifyEmbedUtilsKt.embedMessage(RobertifyLocaleMessageKt.GeneralMessages.GUILD_COMMAND_ONLY).build()
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
        if (!GuildConfigKt(guild).isBannedUser(event.user.idLong)) return true
        event.replyEmbeds(
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.GeneralMessages.BANNED_FROM_COMMANDS
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
        return !(!GeneralUtilsKt.checkPremium(event.guild!!, event) && user.idLong != 276778018440085505L)
    }

    protected open fun premiumBotCheck(event: SlashCommandInteractionEvent): Boolean {
        if (!ConfigKt.PREMIUM_BOT) return true
        val guild = event.guild ?: return true
        if (!GuildConfigKt(guild).isPremium()) {
            event.replyEmbeds(
                RobertifyEmbedUtilsKt.embedMessageWithTitle(
                    guild,
                    RobertifyLocaleMessageKt.GeneralMessages.PREMIUM_EMBED_TITLE,
                    RobertifyLocaleMessageKt.GeneralMessages.PREMIUM_INSTANCE_NEEDED
                ).build()
            )
                .addActionRow(
                    Button.link(
                        "https://robertify.me/premium",
                        LocaleManagerKt.getLocaleManager(guild)
                            .getMessage(RobertifyLocaleMessageKt.GeneralMessages.PREMIUM_UPGRADE_BUTTON)
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
        val guild = event.guild!!
        if (info.djOnly
            && !GeneralUtilsKt.hasPerms(guild, event.member, PermissionKt.ROBERTIFY_DJ)
            && !GeneralUtilsKt.isDeveloper(event.user.idLong)
        ) {
            event.replyEmbeds(
                RobertifyEmbedUtilsKt.embedMessage(
                    guild,
                    BotConstantsKt.getInsufficientPermsMessage(guild, PermissionKt.ROBERTIFY_DJ)
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
        val guild = event.guild!!
        if (info.adminOnly
            && !GeneralUtilsKt.hasPerms(guild, event.member, PermissionKt.ROBERTIFY_ADMIN)
            && !GeneralUtilsKt.isDeveloper(event.user.idLong)
        ) {
            event.replyEmbeds(
                RobertifyEmbedUtilsKt.embedMessage(
                    guild,
                    BotConstantsKt.getInsufficientPermsMessage(guild, PermissionKt.ROBERTIFY_ADMIN)
                ).build()
            )
                .setEphemeral(true)
                .queue()
            return false
        }
        return true
    }

    protected open fun predicateCheck(event: SlashCommandInteractionEvent): Boolean {
        if (info.checkPermission == null) return true
        return if (GeneralUtilsKt.isDeveloper(event.id)) true else info.checkPermission!!.invoke(event)
    }

    protected open fun botPermsCheck(event: SlashCommandInteractionEvent): Boolean {
        if (info.botRequiredPermissions.isEmpty()) return true
        val guild = event.guild ?: return true
        val self = guild.selfMember
        if (!self.hasPermission(info.botRequiredPermissions)) {
            event.replyEmbeds(
                RobertifyEmbedUtilsKt.embedMessage(
                    guild,
                    RobertifyLocaleMessageKt.GeneralMessages.SELF_INSUFFICIENT_PERMS_ARGS,
                    Pair(
                        "{permissions}",
                        info.botRequiredPermissions.asString()
                    )
                ).build()
            )
                .setEphemeral(RobertifyEmbedUtilsKt.getEphemeralState(event.channel.asGuildMessageChannel()))
                .queue()
            return false
        }
        return true
    }

    protected open fun botEmbedCheck(event: SlashCommandInteractionEvent): Boolean {
        val guild = event.guild ?: return true
        if (!guild.selfMember.hasPermission(event.guildChannel, Permission.MESSAGE_EMBED_LINKS)) {
            event.reply(
                LocaleManagerKt.getLocaleManager(guild)
                    .getMessage(RobertifyLocaleMessageKt.GeneralMessages.NO_EMBED_PERMS)
            )
                .queue()
            return false
        }
        return true
    }

    protected open fun restrictedChannelCheck(event: SlashCommandInteractionEvent): Boolean {
        val guild = event.guild ?: return true
        val togglesConfig = TogglesConfigKt(guild)
        val config = RestrictedChannelsConfigKt(guild)
        if (!togglesConfig.getToggle(ToggleKt.RESTRICTED_TEXT_CHANNELS)) return true
        if (!config.isRestrictedChannel(event.channel.idLong, RestrictedChannelsConfigKt.ChannelType.TEXT_CHANNEL)
            && !GeneralUtilsKt.hasPerms(guild, event.member, PermissionKt.ROBERTIFY_ADMIN)
            && !GeneralUtilsKt.isDeveloper(event.user.idLong)
        ) {
            event.replyEmbeds(
                RobertifyEmbedUtilsKt.embedMessage(
                    guild, RobertifyLocaleMessageKt.GeneralMessages.CANT_BE_USED_IN_CHANNEL_ARGS,
                    Pair(
                        "{channels}", GeneralUtilsKt.listOfIDsToMentions(
                            guild,
                            config.getRestrictedChannels(RestrictedChannelsConfigKt.ChannelType.TEXT_CHANNEL),
                            GeneralUtilsKt.Companion.Mentioner.CHANNEL
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
        if (!info.isPrivate) return true
        if (!BotDBCacheKt.instance.isDeveloper(event.user.idLong)) {
            event.replyEmbeds(
                RobertifyEmbedUtilsKt.embedMessage(
                    event.guild,
                    RobertifyLocaleMessageKt.GeneralMessages.INSUFFICIENT_PERMS_NO_ARGS
                ).build()
            )
                .setEphemeral(true)
                .queue()
            return false
        }
        return true
    }

}