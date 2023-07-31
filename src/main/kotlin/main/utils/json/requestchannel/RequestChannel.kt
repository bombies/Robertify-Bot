package main.utils.json.requestchannel

import org.json.JSONObject

data class RequestChannel(val channelId: Long, val messageId: Long, val config: JSONObject)
