package main.audiohandlers.models

import main.utils.GeneralUtilsKt

data class RequesterKt(val id: String, val trackId: String) {

    override fun toString(): String = GeneralUtilsKt.toMention(
        id = id,
        mentioner = GeneralUtilsKt.Companion.Mentioner.USER
    )
}
