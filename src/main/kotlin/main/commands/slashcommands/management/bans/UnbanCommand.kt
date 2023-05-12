package main.commands.slashcommands.management.bans

import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.constants.RobertifyPermission
import main.utils.GeneralUtils.dmEmbed
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.Command
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.json.guildconfig.GuildConfig
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.UnbanMessages
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

class UnbanCommand : AbstractSlashCommand(
    Command(
        name = "unban",
        description = "Unban a user from the bot",
        options = listOf(
            CommandOption(
                type = OptionType.USER,
                name = "user",
                description = "The user to unban"
            )
        ),
        _requiredPermissions = listOf(RobertifyPermission.ROBERTIFY_BAN)
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val user = event.getRequiredOption("user").asMember ?: run {
            return event.replyEmbed {
                embed(GeneralMessages.INVALID_ARGS)
            }.setEphemeral(true)
                .queue()
        }

        event.replyEmbed { handleUnban(user) }.queue()
    }

    private fun handleUnban(user: Member): MessageEmbed {
        val guild = user.guild
        val config = GuildConfig(guild)
        if (!config.isBannedUser(user.idLong))
            return RobertifyEmbedUtils.embedMessage(guild, UnbanMessages.USER_NOT_BANNED)
                .build()

        config.unbanUser(user.idLong)
        user.user.dmEmbed(
            UnbanMessages.USER_UNBANNED,
            Pair("{server}", guild.name)
        )

        return RobertifyEmbedUtils.embedMessage(
            guild,
            UnbanMessages.USER_UNBANNED_RESPONSE,
            Pair("{user}", user.asMention)
        ).build()
    }
}