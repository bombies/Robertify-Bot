package main.commands.slashcommands.management.bans

import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.constants.RobertifyPermission
import main.main.Listener
import main.utils.GeneralUtils
import main.utils.GeneralUtils.dmEmbed
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.json.guildconfig.GuildConfig
import main.utils.locale.messages.BanMessages
import main.utils.locale.messages.GeneralMessages
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

class BanCommand : AbstractSlashCommand(
    SlashCommand(
        name = "ban",
        description = "Ban a user from using the bot.",
        options = listOf(
            CommandOption(
                type = OptionType.USER,
                name = "user",
                description = "The user to ban"
            ),
            CommandOption(
                name = "duration",
                description = "The duration the user should be banned for",
                required = false
            )
        ),
        _requiredPermissions = listOf(RobertifyPermission.ROBERTIFY_BAN)
    )
) {

    override fun handle(event: SlashCommandInteractionEvent) {
        val user = event.getRequiredOption("user").asMember ?: run {
            return event.replyEmbed {
                embed(GeneralMessages.INVALID_ARGS)
            }.setEphemeral(true)
                .queue()
        }
        val duration = event.getOption("duration")?.asString

        event.replyEmbed {
            handleBan(user, event.member!!, duration)
        }.queue()
    }

    private fun handleBan(user: Member, mod: Member, duration: String?): MessageEmbed {
        val guild = user.guild
        if (duration != null && !GeneralUtils.isValidDuration(duration))
            return RobertifyEmbedUtils.embedMessage(guild, BanMessages.INVALID_BAN_DURATION)
                .build()

        val bannedUntil = if (duration != null) GeneralUtils.getFutureTime(duration) else null

        if (GeneralUtils.hasPerms(guild, user, RobertifyPermission.ROBERTIFY_ADMIN))
            return RobertifyEmbedUtils.embedMessage(guild, BanMessages.CANNOT_BAN_ADMIN)
                .build()

        if (GeneralUtils.isDeveloper(user.idLong))
            return RobertifyEmbedUtils.embedMessage(guild, BanMessages.CANNOT_BAN_DEVELOPER)
                .build()

        val config = GuildConfig(guild)
        if (config.isBannedUser(user.idLong))
            return RobertifyEmbedUtils.embedMessage(guild, BanMessages.USER_ALREADY_BANNED)
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

            RobertifyEmbedUtils.embedMessage(
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
                Pair("{duration}", GeneralUtils.formatDuration(duration!!)),
                Pair("{server}", guild.name)
            )

            Listener.scheduleUnban(guild, user.user)
            RobertifyEmbedUtils.embedMessage(
                guild,
                BanMessages.USER_TEMP_BANNED_RESPONSE,
                Pair("{user}", user.asMention),
                Pair("{duration}", GeneralUtils.formatDuration(duration))
            ).build()
        }
    }
}