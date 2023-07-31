package main.utils.json.restrictedchannels

import main.utils.json.GenericJSONField

enum class RestrictedChannelsConfigField(private val str: String) : GenericJSONField {
    VOICE_CHANNELS("voice_channels"),
    TEXT_CHANNELS("text_channels");

    override fun toString(): String = str
}