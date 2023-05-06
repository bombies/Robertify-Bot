package main.utils.json.eightball

import main.utils.database.mongodb.databases.GuildDBKt
import main.utils.json.AbstractGuildConfigKt
import net.dv8tion.jda.api.entities.Guild

class EightBallConfigKt(private val guild: Guild) : AbstractGuildConfigKt(guild) {

    val responses: List<String>
        get() {
            val obj = getGuildObject()
                .getJSONArray(GuildDBKt.Field.EIGHT_BALL_ARRAY.toString())
            val responses: MutableList<String> = ArrayList()
            for (i in 0 until obj.length()) responses.add(obj.getString(i))
            return responses
        }

    operator fun plus(response: String) {
        val obj = getGuildObject()
        val array = obj.getJSONArray(GuildDBKt.Field.EIGHT_BALL_ARRAY.toString())
            .put(response)
        cache.setField(guild.idLong, GuildDBKt.Field.EIGHT_BALL_ARRAY, array)
    }

    fun addResponse(response: String) = plus(response)

    operator fun minus(responseIndex: Int): String {
        val obj = getGuildObject()
        val array = obj.getJSONArray(GuildDBKt.Field.EIGHT_BALL_ARRAY.toString())
        val value = array.remove(responseIndex).toString()
        cache.setField(guild.idLong, GuildDBKt.Field.EIGHT_BALL_ARRAY, array)
        return value
    }

    fun removeResponse(responseIndex: Int) = minus(responseIndex)

    operator fun unaryMinus() {
        val obj = getGuildObject()
        val array = obj.getJSONArray(GuildDBKt.Field.EIGHT_BALL_ARRAY.toString())
        array.clear()
        cache.setField(guild.idLong, GuildDBKt.Field.EIGHT_BALL_ARRAY, array)
    }

    fun removeAllResponses() = unaryMinus()

    override fun update() {
        // Nothing
    }
}