package main.utils.json.eightball

import main.utils.database.mongodb.databases.GuildDB
import main.utils.json.AbstractGuildConfig
import net.dv8tion.jda.api.entities.Guild

class EightBallConfig(private val guild: Guild) : AbstractGuildConfig(guild) {

    suspend fun getResponses(): List<String> {
        return getGuildModel().eight_ball ?: emptyList()
    }

    suspend fun addResponse(response: String) {
        cache.updateGuild(guild.id) {
            eight_ball {
                add(response)
            }
        }
    }

    suspend fun removeResponse(responseIndex: Int): String {
        var removed = getResponses()[responseIndex]

        cache.updateGuild(guild.id) {
            eight_ball {
                removed = removeAt(responseIndex)
            }
        }

        return removed
    }

    suspend fun clearResponses() {
        cache.updateGuild(guild.id) {
            eight_ball {
                clear()
            }
        }
    }

    override suspend fun update() {
        // Nothing
    }
}