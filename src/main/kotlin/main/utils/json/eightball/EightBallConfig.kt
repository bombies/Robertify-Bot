package main.utils.json.eightball

import main.utils.json.AbstractGuildConfig
import net.dv8tion.jda.api.entities.Guild

class EightBallConfig(private val guild: Guild) : AbstractGuildConfig(guild) {

    fun getResponses(): List<String> {
        return getGuildModel().eight_ball
    }

    fun addResponse(response: String) {
        cache.updateGuild(guild.id) {
            eight_ball {
                add(response)
            }
        }
    }

    fun removeResponse(responseIndex: Int): String {
        var removed = getResponses()[responseIndex]

        cache.updateGuild(guild.id) {
            eight_ball {
                removed = removeAt(responseIndex)
            }
        }

        return removed
    }

    fun clearResponses() {
        cache.updateGuild(guild.id) {
            eight_ball {
                clear()
            }
        }
    }

    override fun update() {
        // Nothing
    }
}