package main.utils.json.themes

import main.constants.RobertifyTheme
import main.utils.database.mongodb.cache.GuildDBCache
import main.utils.database.mongodb.databases.GuildDB
import main.utils.json.AbstractGuildConfig
import main.utils.json.getIndexOfObjectInArray
import net.dv8tion.jda.api.entities.Guild
import org.bson.Document
import org.json.JSONException
import java.util.*

class ThemesConfig(private val guild: Guild) : AbstractGuildConfig(guild) {
    
    suspend fun getTheme(): RobertifyTheme {
        return RobertifyTheme.parse(getGuildModel().theme)
    }
    
    suspend fun setTheme(theme: RobertifyTheme) {
        cache.updateGuild(guild.id) {
            this.theme = theme.name.lowercase()
        }
    }

    override suspend fun update() {
        if (!guildHasInfo()) loadGuild()
        val cacheArr = GuildDBCache.ins.getCache()
        val obj = cacheArr.getJSONObject(getIndexOfObjectInArray(cacheArr, GuildDB.Field.GUILD_ID, guild.idLong))
        if (!obj.has(ThemesConfigField.THEME.toString())) {
            obj.put(ThemesConfigField.THEME.toString(), RobertifyTheme.GREEN.name.lowercase(Locale.getDefault()))
        }
        cache.updateCache(guild.idLong.toString(), Document.parse(obj.toString()))
    }
}