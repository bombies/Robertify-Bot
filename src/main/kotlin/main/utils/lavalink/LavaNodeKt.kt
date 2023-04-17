package main.utils.lavalink

import java.net.URI

data class LavaNodeKt(
    private val host: String,
    private val port: String,
    val password: String
) {

    fun getURI(): URI =
        URI.create("ws://$host${ if (port.isNotEmpty() && port.isNotBlank()) ":$port" else ""}")

    override fun toString(): String =
        "LavaNode{host='$host', port='$port', password='$password'}"

}
