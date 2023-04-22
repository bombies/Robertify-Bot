package main.audiohandlers.models

import kotlinx.serialization.Serializable
import main.utils.GeneralUtilsKt

@Serializable
data class RequesterKt(val id: String, val trackId: String) {

    override fun toString(): String = GeneralUtilsKt.toMention(
        id = id,
        mentioner = GeneralUtilsKt.Companion.Mentioner.USER
    )
}
