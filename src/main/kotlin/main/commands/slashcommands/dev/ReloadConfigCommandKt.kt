package main.commands.slashcommands.dev

import dev.minn.jda.ktx.util.SLF4J
import main.main.ConfigKt
import main.main.RobertifyKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.RobertifyEmbedUtilsKt.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.locale.LocaleManagerKt
import main.utils.managers.RandomMessageManagerKt
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class ReloadConfigCommandKt : AbstractSlashCommandKt(CommandKt(
    name = "reload",
    description = "Reload all config files.",
    developerOnly = true
)) {

    companion object {
        private val logger by SLF4J
    }

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        event.deferReply(true).queue()

        try {
            ConfigKt.reload()
            LocaleManagerKt.reloadLocales()
            RobertifyKt.initVoteSiteAPIs()
            RandomMessageManagerKt.chance = ConfigKt.RANDOM_MESSAGE_CHANCE

            event.hook.sendEmbed(guild, "Successfully reloaded all configs!")
                .queue()
        } catch (e: Exception) {
            logger.error("There was an unexpected error!", e)
            event.hook.sendEmbed(guild, "There was an unexpected error!\n```${e.message ?: "No message provided."}```")
                .queue()
        }

    }
}