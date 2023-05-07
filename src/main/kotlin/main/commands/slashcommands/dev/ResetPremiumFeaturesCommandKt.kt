package main.commands.slashcommands.dev

import dev.minn.jda.ktx.messages.send
import dev.minn.jda.ktx.util.SLF4J
import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.utils.RobertifyEmbedUtilsKt.Companion.sendEmbed
import main.utils.api.robertify.models.RobertifyPremiumKt
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.component.interactions.slashcommand.models.SubCommandKt
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

class ResetPremiumFeaturesCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "resetpremiumfeatures",
        description = "Reset premium features for a specific guild or all guilds.",
        developerOnly = true,
        subcommands = listOf(
            SubCommandKt(
                name = "all",
                description = "Reset premium features for all guilds."
            ),
            SubCommandKt(
                name = "guild",
                description = "Reset premium features for a specific guild.",
                options = listOf(
                    CommandOptionKt(
                        name = "id",
                        description = "The ID of the guild"
                    )
                )
            )
        )
    )
) {

    companion object {
        private val logger by SLF4J
    }

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.deferReply(true).queue()

        when (event.subcommandName) {
            "all" -> handleAll(event)
            "guild" -> handleGuild(event)
        }
    }

    private fun handleAll(event: SlashCommandInteractionEvent) {
        event.jda.shardManager!!.guildCache.forEach { guild ->
            RobertifyPremiumKt.resetPremiumFeatures(guild)
            logger.info("Reset all premium features for ${guild.name}")
        }
        event.hook.sendEmbed(event.guild, "Reset premium features for all guilds!")
            .queue()
    }

    private fun handleGuild(event: SlashCommandInteractionEvent) {
        val guildId = event.getRequiredOption("id").asString
        val eventGuild = event.guild!!
        val guild = event.jda.shardManager!!.getGuildById(guildId)
            ?: return event.hook.sendEmbed(eventGuild, "There was no such guild with the ID: $guildId")
                .queue()

        RobertifyPremiumKt.resetPremiumFeatures(guild)
        logger.info("Reset all premium features for ${guild.name}")
        event.hook.sendEmbed(eventGuild, "Successfully reset all premium features for ${guild.name}")
            .queue()
    }

}