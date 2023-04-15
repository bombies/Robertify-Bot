package main.utils.json.restrictedchannels

import main.utils.json.GenericJSONFieldKt

enum class RestrictedChannelsConfigFieldKt(private val str: String) : GenericJSONFieldKt {
    VOICE_CHANNELS("voice_channels"),
    TEXT_CHANNELS("text_channels");

    override fun toString(): String = str
}