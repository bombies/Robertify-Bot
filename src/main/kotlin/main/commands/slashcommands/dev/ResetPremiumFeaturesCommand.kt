package main.commands.slashcommands.dev

import dev.minn.jda.ktx.util.SLF4J
import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.api.robertify.models.RobertifyPremium
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.component.interactions.slashcommand.models.SubCommand
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class ResetPremiumFeaturesCommand : AbstractSlashCommand(
    SlashCommand(
        name = "resetpremiumfeatures",
        description = "Reset premium features for a specific guild or all guilds.",
        developerOnly = true,
        subcommands = listOf(
            SubCommand(
                name = "all",
                description = "Reset premium features for all guilds."
            ),
            SubCommand(
                name = "guild",
                description = "Reset premium features for a specific guild.",
                options = listOf(
                    CommandOption(
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

    override fun handle(event: SlashCommandInteractionEvent) {
        event.deferReply(true).queue()

        when (event.subcommandName) {
            "all" -> handleAll(event)
            "guild" -> handleGuild(event)
        }
    }

    private fun handleAll(event: SlashCommandInteractionEvent) {
        event.jda.shardManager!!.guildCache.forEach { guild ->
            RobertifyPremium.resetPremiumFeatures(guild)
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

        RobertifyPremium.resetPremiumFeatures(guild)
        logger.info("Reset all premium features for ${guild.name}")
        event.hook.sendEmbed(eventGuild, "Successfully reset all premium features for ${guild.name}")
            .queue()
    }

}