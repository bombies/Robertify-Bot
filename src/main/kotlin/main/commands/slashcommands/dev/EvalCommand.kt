package main.commands.slashcommands.dev

import dev.minn.jda.ktx.util.SLF4J
import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.main.Robertify
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import me.duncte123.botcommons.web.WebUtils
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import okhttp3.OkHttpClient
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.ScriptException

class EvalCommand : AbstractSlashCommand(
    SlashCommand(
        name = "eval",
        description = "Spooky scary evals...",
        developerOnly = true,
        options = listOf(
            CommandOption(
                name = "src",
                description = "The source code to evaluate"
            )
        )
    )
) {

    companion object {
        val logger by SLF4J
    }

    val engine: ScriptEngine = ScriptEngineManager().getEngineByName("nashorn")

    init {
        try {
            engine.eval(
                """
            var imports = new JavaImporter(
                java.io,
                java.lang,
                java.util,
                Packages.net.dv8tion.jda.api,
                Packages.net.dv8tion.jda.api.entities,
                Packages.net.dv8tion.jda.api.entities.impl,
                Packages.net.dv8tion.jda.api.managers,
                Packages.net.dv8tion.jda.api.managers.impl,
                Packages.net.dv8tion.jda.api.utils
            );
        """.trimIndent()
            )
        } catch (e: ScriptException) {
            logger.error("Script error occurred!", e)
        }
    }

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val src = event.getRequiredOption("src").asString
        event.deferReply(true).queue()

        try {
            engine.put("event", event)
            engine.put("channel", event.channel.asGuildMessageChannel())
            engine.put("jda", event.jda)
            engine.put("shards", event.jda.shardManager!!)
            engine.put("guild", event.guild!!)
            engine.put("member", event.member!!)
            engine.put("link", Robertify.lavalink)
            engine.put("requester", WebUtils.ins)
            engine.put("http", OkHttpClient())
            // TODO: Add RobertifyAPI

            val out = engine.eval(
                "(function() {\n" +
                        " with (imports) { \n" +
                        src +
                        " \n}" +
                        "\n})();"
            )

            if (out != null) {
                event.hook.sendEmbed {
                    RobertifyEmbedUtils.embedMessage(guild, "```java\n${src}```")
                        .addField("Result", out.toString(), false)
                        .build()
                }.queue()
            } else event.hook.sendEmbed(guild, "```java\nExecuted without error.```")
        } catch (e: Exception) {
            event.hook.sendEmbed(guild, "```java\n${e.message ?: "Unexpected error"}").queue()
        }
    }
}