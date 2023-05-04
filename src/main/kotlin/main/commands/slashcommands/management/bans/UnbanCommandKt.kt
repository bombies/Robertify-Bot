package main.commands.slashcommands.management.bans

import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.constants.PermissionKt
import main.utils.GeneralUtilsKt.dmEmbed
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.json.guildconfig.GuildConfigKt
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.UnbanMessages
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

class UnbanCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "unban",
        description = "Unban a user from the bot",
        options = listOf(
            CommandOptionKt(
                type = OptionType.USER,
                name = "user",
                description = "The user to unban"
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

        event.replyWithEmbed { handleUnban(user) }.queue()
    }

    private fun handleUnban(user: Member): MessageEmbed {
        val guild = user.guild
        val config = GuildConfigKt(guild)
        if (!config.isBannedUser(user.idLong))
            return RobertifyEmbedUtilsKt.embedMessage(guild, UnbanMessages.USER_NOT_BANNED)
                .build()

        config.unbanUser(user.idLong)
        user.user.dmEmbed(
            UnbanMessages.USER_UNBANNED,
            Pair("{server}", guild.name)
        )

        return RobertifyEmbedUtilsKt.embedMessage(
            guild,
            UnbanMessages.USER_UNBANNED_RESPONSE,
            Pair("{user}", user.asMention)
        ).build()
    }
}