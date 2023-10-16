package main.commands.slashcommands.dev

import dev.minn.jda.ktx.util.SLF4J
import main.main.Config
import main.main.Robertify
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.locale.LocaleManager
import main.utils.managers.RandomMessageManager
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class ReloadConfigCommand : AbstractSlashCommand(SlashCommand(
    name = "reload",
    description = "Reload all config files.",
    developerOnly = true
)) {

    companion object {
        private val logger by SLF4J
    }

    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        event.deferReply(true).queue()

        try {
            Config.reload()
            LocaleManager.reloadLocales()
            Robertify.initVoteSiteAPIs()
            RandomMessageManager.chance = Config.RANDOM_MESSAGE_CHANCE

            event.hook.sendEmbed(guild, "Successfully reloaded all configs!")
                .queue()
        } catch (e: Exception) {
            logger.error("There was an unexpected error!", e)
            event.hook.sendEmbed(guild, "There was an unexpected error!\n```${e.message ?: "No message provided."}```")
                .queue()
        }

    }
}