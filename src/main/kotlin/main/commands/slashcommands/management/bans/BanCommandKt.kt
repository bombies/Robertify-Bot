package main.commands.slashcommands.management.bans

import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.constants.PermissionKt
import main.main.ListenerKt
import main.utils.GeneralUtilsKt
import main.utils.GeneralUtilsKt.dmEmbed
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.json.guildconfig.GuildConfigKt
import main.utils.locale.messages.BanMessages
import main.utils.locale.messages.GeneralMessages
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

class BanCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "ban",
        description = "Ban a user from using the bot.",
        options = listOf(
            CommandOptionKt(
                type = OptionType.USER,
                name = "user",
                description = "The user to ban"
            ),
            CommandOptionKt(
                name = "duration",
                description = "The duration the user should be banned for",
                required = false
            )
        ),
        _requiredPermissions = listOf(PermissionKt.ROBERTIFY_BAN)
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val user = event.getRequiredOption("user").asMember ?: run {
            return event.replyWithEmbed(event.guild!!) {
                embed(GeneralMessages.INVALID_ARGS)
            }.setEphemeral(true)
                .queue()
        }
        val duration = event.getOption("duration")?.asString

        event.replyWithEmbed(event.guild!!) {
            handleBan(user, event.member!!, duration)
        }.queue()
    }

    private fun handleBan(user: Member, mod: Member, duration: String?): MessageEmbed {
        val guild = user.guild
        if (duration != null && !GeneralUtilsKt.isValidDuration(duration))
            return RobertifyEmbedUtilsKt.embedMessage(guild, BanMessages.INVALID_BAN_DURATION)
                .build()

        val bannedUntil = if (duration != null) GeneralUtilsKt.getFutureTime(duration) else null

        if (GeneralUtilsKt.hasPerms(guild, user, PermissionKt.ROBERTIFY_ADMIN))
            return RobertifyEmbedUtilsKt.embedMessage(guild, BanMessages.CANNOT_BAN_ADMIN)
                .build()

        if (GeneralUtilsKt.isDeveloper(user.idLong))
            return RobertifyEmbedUtilsKt.embedMessage(guild, BanMessages.CANNOT_BAN_DEVELOPER)
                .build()

        val config = GuildConfigKt(guild)
        if (config.isBannedUser(user.idLong))
            return RobertifyEmbedUtilsKt.embedMessage(guild, BanMessages.USER_ALREADY_BANNED)
                .build()

        return if (bannedUntil == null) {
            // Handle permanent bans
            config.banUser(
                uid = user.idLong,
                modId = mod.idLong,
                bannedAt = System.currentTimeMillis(),
                bannedUntil = -1
            )

            user.user.dmEmbed(
                BanMessages.USER_PERM_BANNED,
                Pair("{server}", guild.name)
            )

            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                BanMessages.USER_PERM_BANNED_RESPONSE,
                Pair("{user}", user.asMention)
            ).build()
        } else {
            // Handle temporary bans
            config.banUser(
                uid = user.idLong,
                modId = mod.idLong,
                bannedAt = System.currentTimeMillis(),
                bannedUntil = bannedUntil
            )

            user.user.dmEmbed(
                BanMessages.USER_TEMP_BANNED,
                Pair("{duration}", GeneralUtilsKt.formatDuration(duration!!)),
                Pair("{server}", guild.name)
            )

            ListenerKt.scheduleUnban(guild, user.user)
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                BanMessages.USER_TEMP_BANNED_RESPONSE,
                Pair("{user}", user.asMention),
                Pair("{duration}", GeneralUtilsKt.formatDuration(duration))
            ).build()
        }
    }
}