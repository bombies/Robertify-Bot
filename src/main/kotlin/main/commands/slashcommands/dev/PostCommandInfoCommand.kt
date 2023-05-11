package main.commands.slashcommands.dev

import dev.minn.jda.ktx.util.SLF4J
import io.ktor.client.statement.*
import io.ktor.http.*
import main.commands.slashcommands.SlashCommandManagerKt
import main.main.RobertifyKt
import main.utils.RobertifyEmbedUtilsKt.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class PostCommandInfoCommand : AbstractSlashCommandKt(
    CommandKt(
        name = "postcommandinfo",
        description = "Post all command info to the Robertify API!",
        developerOnly = true
    )
) {

    companion object {
        private val logger by SLF4J
    }

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val commandManager = SlashCommandManagerKt
        val commands = commandManager.globalCommands

        val body = JSONObject()
        val cmdArr = JSONArray()

        commands.forEachIndexed { i, command ->
            cmdArr.put(
                JSONObject()
                    .put("id", i)
                    .put("name", command.info.name)
                    .put("description", command.info.description)
                    .put("category", commandManager.getCommandType(command)!!.name.lowercase())
            )
        }

        body.put("commands", cmdArr)
        event.deferReply(true).queue()

        val guild = event.guild
        val response = RobertifyKt.externalApi.postCommandInfo(body)
        if (response != null && response.status == HttpStatusCode.Created)
            event.hook.sendEmbed(guild, "Posted!")
                .queue()
        else if (response != null) {
            try {
                val err = JSONObject(response.bodyAsText()).getString("message")
                event.hook.sendEmbed(
                    guild,
                    "There was an issue attempting to post commands! Check console for more information.\n\nError Message:\n```$err```\nError Code:`${response.status.value}`"
                )
                    .queue()
                logger.error("There was an error when trying to POST commands: $err")
            } catch (e: JSONException) {
                event.hook.sendEmbed(
                    guild,
                    "There was an issue attempting to post commands! Check console for more information.\n\nError Code:`${response.status.value}`"
                )
                    .queue()
                logger.error(response.bodyAsText())

            }
        } else event.hook.sendEmbed(guild, "Could not get a response! Are you sure there's a connection to the API?")
            .queue()
    }
}