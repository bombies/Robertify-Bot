package main.utils.json.themes

import main.constants.RobertifyTheme
import main.utils.database.mongodb.cache.GuildDBCache
import main.utils.database.mongodb.databases.GuildDB
import main.utils.json.AbstractGuildConfig
import net.dv8tion.jda.api.entities.Guild
import org.bson.Document
import org.json.JSONException
import java.util.*

class ThemesConfig(private val guild: Guild) : AbstractGuildConfig(guild) {

    var theme: RobertifyTheme
        get() {
            if (!guildHasInfo()) loadGuild()
            val theme: String = try {
                getGuildObject().getString(ThemesConfigField.THEME.toString())
            } catch (e: JSONException) {
                update()
                getGuildObject().getString(ThemesConfigField.THEME.toString())
            }
            return RobertifyTheme.parse(theme.lowercase(Locale.getDefault()))
        }
        set(value) {
            if (!guildHasInfo()) loadGuild()
            val obj = getGuildObject()
            obj.put(ThemesConfigField.THEME.toString(), value.name.lowercase(Locale.getDefault()))
            cache.updateGuild(obj, guild.idLong)
        }

    override fun update() {
        if (!guildHasInfo()) loadGuild()
        val cacheArr = GuildDBCache.ins.getCache()
        val obj = cacheArr.getJSONObject(getIndexOfObjectInArray(cacheArr, GuildDB.Field.GUILD_ID, guild.idLong))
        if (!obj.has(ThemesConfigField.THEME.toString())) {
            obj.put(ThemesConfigField.THEME.toString(), RobertifyTheme.GREEN.name.lowercase(Locale.getDefault()))
        }
        cache.updateCache(guild.idLong.toString(), Document.parse(obj.toString()))
    }
}