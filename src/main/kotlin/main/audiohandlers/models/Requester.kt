package main.audiohandlers.models

import kotlinx.serialization.Serializable
import main.utils.GeneralUtils

@Serializable
data class Requester(val id: String, val trackId: String) {

    suspend fun toMention(): String = GeneralUtils.toMention(
        id = id,
        mentioner = GeneralUtils.Mentioner.USER
    )
}
