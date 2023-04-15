package main.utils.json.eightball

import main.utils.database.mongodb.databases.GuildDBKt
import main.utils.json.AbstractGuildConfigKt
import net.dv8tion.jda.api.entities.Guild

class EightBallConfigKt(private val guild: Guild) : AbstractGuildConfigKt(guild) {

    fun addResponse(response: String?) {
        val obj = getGuildObject()
        val array = obj.getJSONArray(GuildDBKt.Field.EIGHT_BALL_ARRAY.toString())
            .put(response)
        cache.setField(guild.idLong, GuildDBKt.Field.EIGHT_BALL_ARRAY, array)
    }

    fun removeResponse(responseIndex: Int) {
        val obj = getGuildObject()
        val array = obj.getJSONArray(GuildDBKt.Field.EIGHT_BALL_ARRAY.toString())
        array.remove(responseIndex)
        cache.setField(guild.idLong, GuildDBKt.Field.EIGHT_BALL_ARRAY, array)
    }

    fun removeAllResponses() {
        val obj = getGuildObject()
        val array = obj.getJSONArray(GuildDBKt.Field.EIGHT_BALL_ARRAY.toString())
        array.clear()
        cache.setField(guild.idLong, GuildDBKt.Field.EIGHT_BALL_ARRAY, array)
    }

    fun getResponses(): List<String> {
        val obj = getGuildObject()
            .getJSONArray(GuildDBKt.Field.EIGHT_BALL_ARRAY.toString())
        val responses: MutableList<String> = ArrayList()
        for (i in 0 until obj.length()) responses.add(obj.getString(i))
        return responses
    }

    override fun update() {
        // Nothing
    }
}