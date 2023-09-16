package main.utils.json.toggles

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import main.commands.slashcommands.SlashCommandManager
import main.constants.Toggle
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.database.mongodb.cache.GuildDBCache
import main.utils.database.mongodb.cache.redis.guild.DJTogglesModel
import main.utils.database.mongodb.cache.redis.guild.GuildDatabaseModel
import main.utils.database.mongodb.cache.redis.guild.LogTogglesModel
import main.utils.database.mongodb.cache.redis.guild.TogglesModel
import main.utils.database.mongodb.databases.GuildDB
import main.utils.json.AbstractGuildConfig
import main.utils.json.getIndexOfObjectInArray
import main.utils.json.logs.LogType
import net.dv8tion.jda.api.entities.Guild
import org.bson.Document
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class TogglesConfig(private val guild: Guild) : AbstractGuildConfig(guild) {

    companion object {
        fun getDefaultToggleObject(): JSONObject {
            val toggleObj = JSONObject()
            for (toggle in Toggle.values()) try {
                getToggles(toggleObj, toggle)
            } catch (e: JSONException) {
                for (errToggles in Toggle.values()) when (errToggles) {
                    Toggle.RESTRICTED_VOICE_CHANNELS, Toggle.RESTRICTED_TEXT_CHANNELS -> toggleObj.put(
                        errToggles.toString(),
                        false
                    )

                    else -> toggleObj.put(errToggles.toString(), true)
                }
            }
            return toggleObj
        }

        private fun getToggles(toggleObj: JSONObject, toggle: Toggle) {
            if (!toggleObj.has(toggle.toString())) when (toggle) {
                Toggle.RESTRICTED_VOICE_CHANNELS, Toggle.RESTRICTED_TEXT_CHANNELS ->
                    toggleObj.put(toggle.toString(), false)

                else -> toggleObj.put(toggle.toString(), true)
            }

            if (!toggleObj.has(Toggle.TogglesConfigField.DJ_TOGGLES.toString())) {
                val djTogglesObj = JSONObject()
                for (musicCommand in SlashCommandManager.musicCommands) djTogglesObj.put(
                    musicCommand.info.name.lowercase(
                        Locale.getDefault()
                    ), false
                )
                toggleObj.put(Toggle.TogglesConfigField.DJ_TOGGLES.toString(), djTogglesObj)
            } else {
                val djTogglesObj = toggleObj.getJSONObject(Toggle.TogglesConfigField.DJ_TOGGLES.toString())
                for (musicCommand in SlashCommandManager.musicCommands)
                    if (!djTogglesObj.has(musicCommand.info.name))
                        djTogglesObj.put(musicCommand.info.name, false)
                toggleObj.put(Toggle.TogglesConfigField.DJ_TOGGLES.toString(), djTogglesObj)
            }

            if (!toggleObj.has(Toggle.TogglesConfigField.LOG_TOGGLES.toString())) {
                val logObj = JSONObject()
                for (type in LogType.values()) logObj.put(type.name.lowercase(Locale.getDefault()), true)
                toggleObj.put(Toggle.TogglesConfigField.LOG_TOGGLES.toString(), logObj)
            } else {
                val logObj = toggleObj.getJSONObject(Toggle.TogglesConfigField.LOG_TOGGLES.toString())
                for (type in LogType.values()) if (!logObj.has(type.name.lowercase(Locale.getDefault()))) logObj.put(
                    type.name.lowercase(
                        Locale.getDefault()
                    ), true
                )
                toggleObj.put(Toggle.TogglesConfigField.LOG_TOGGLES.toString(), logObj)
            }
        }
    }

    suspend fun get(toggle: Toggle): Boolean {
        if (!guildHasInfo()) loadGuild()

        val toggles = getTogglesJson();
        return try {
            toggles.getBoolean(toggle.toString())
        } catch (e: JSONException) {
            if (e.message!!.contains("is not a")) {
                toggles.getBoolean(toggle.toString())
            } else {
                val togglesObject = getTogglesJson()
                togglesObject.put(toggle.toString(), true)

                cache.updateGuild(guild.id) {
                    this.toggles = Json.decodeFromString(togglesObject.toString())
                }

                true
            }
        }
    }

    suspend fun getToggle(toggle: Toggle): Boolean = get(toggle)

    suspend fun getDJToggles(): DJTogglesModel {
        return getToggles().dj_toggles
    }

    suspend fun getLogToggles(): LogTogglesModel {
        return getToggles().log_toggles
    }

    suspend fun getDJToggle(cmd: AbstractSlashCommand): Boolean {
        if (!SlashCommandManager.isMusicCommand(cmd))
            return false

        val djToggles = getDJToggles().toJsonObject()

        return if (!djToggles.has(cmd.info.name.lowercase())) {
            setDJToggle(cmd, false)
            return false
        } else djToggles.getBoolean(cmd.info.name.lowercase())
    }

    suspend fun setToggle(toggle: Toggle, value: Boolean) {
        val guildModel = getGuildModel().toJsonObject();
        guildModel.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString())
            .put(toggle.toString(), value)
        val newModel = Json.decodeFromString<GuildDatabaseModel>(guildModel.toString())
        cache.updateGuild(newModel)
    }

    suspend fun setDJToggle(command: AbstractSlashCommand, value: Boolean) {
        val guildModel = getGuildModel().toJsonObject();
        guildModel.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString())
            .getJSONObject(GuildDB.Field.TOGGLES_DJ.toString())
            .put(command.info.name, value)
        val newModel = Json.decodeFromString<GuildDatabaseModel>(guildModel.toString())
        cache.updateGuild(newModel)
    }

    suspend fun setDJToggle(commands: List<AbstractSlashCommand>, value: Boolean) {
        val guildModel = getGuildModel().toJsonObject();
        val djObj = guildModel.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString())
            .getJSONObject(GuildDB.Field.TOGGLES_DJ.toString())
        commands.forEach { command -> djObj.put(command.info.name, value) }
        val newModel = Json.decodeFromString<GuildDatabaseModel>(guildModel.toString())
        cache.updateGuild(newModel)
    }

    suspend fun setLogToggle(type: LogType, state: Boolean) {
        if (!isLogToggleSet(type)) update()
        val obj = getGuildModel().toJsonObject()
        obj.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString())
            .getJSONObject(GuildDB.Field.TOGGLES_LOGS.toString())
            .put(type.name.lowercase(Locale.getDefault()), state)
        val newModel = Json.decodeFromString<GuildDatabaseModel>(obj.toString())
        cache.updateGuild(newModel)
    }

    suspend fun getLogToggle(type: LogType): Boolean {
        if (!isLogToggleSet(type)) update()
        val obj = getGuildModel().toJsonObject()
        return obj.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString())
            .getJSONObject(GuildDB.Field.TOGGLES_LOGS.toString())
            .getBoolean(type.name.lowercase(Locale.getDefault()))
    }

    suspend fun isDJToggleSet(cmd: AbstractSlashCommand): Boolean =
        getDJToggles().toJsonObject().has(cmd.info.name.lowercase())


    suspend fun isDJToggleSet(cmd: String): Boolean =
        getDJToggles().toJsonObject().has(cmd.lowercase(Locale.getDefault()))

    suspend fun isLogToggleSet(type: LogType): Boolean = try {
        getLogToggles().toJsonObject().has(type.name.lowercase(Locale.getDefault()))
    } catch (e: JSONException) {
        false
    }

    suspend fun getToggles(): TogglesModel {
        return getGuildModel().toggles ?: run {
            update()
            return@run getGuildModel().toggles!!
        }
    }

    suspend fun getTogglesJson(): JSONObject = getToggles().toJsonObject()

    override suspend fun update() {
        if (!guildHasInfo()) loadGuild()
        val cacheArr = GuildDBCache.ins.getCache()
        val `object` = cacheArr.getJSONObject(getIndexOfObjectInArray(cacheArr, GuildDB.Field.GUILD_ID, guild.idLong))
        for (toggle in Toggle.entries) {
            try {
                val toggleObj = `object`.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString())
                getToggles(toggleObj, toggle)
            } catch (e: JSONException) {
                for (errToggles in Toggle.entries) when (errToggles) {
                    Toggle.RESTRICTED_VOICE_CHANNELS, Toggle.RESTRICTED_TEXT_CHANNELS -> `object`.getJSONObject(
                        GuildDB.Field.TOGGLES_OBJECT.toString()
                    )
                        .put(errToggles.toString(), false)

                    else -> `object`.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString())
                        .put(errToggles.toString(), true)
                }
            }
        }
        cache.updateCache(guild.id, Document.parse(`object`.toString()))
    }
}