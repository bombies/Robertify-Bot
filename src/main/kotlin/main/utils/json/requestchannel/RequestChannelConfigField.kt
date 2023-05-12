package main.utils.json.requestchannel

enum class RequestChannelConfigField(private val str: String) {
    CHANNEL_ID("channel_id"),
    QUEUE_MESSAGE_ID("message_id"),
    ORIGINAL_ANNOUNCEMENT_TOGGLE("og_announcement_toggle");

    override fun toString(): String = str

}