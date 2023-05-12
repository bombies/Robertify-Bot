package main.utils.lavalink

import java.net.URI

data class LavaNode(
    val name: String = "Lava Node",
    private val host: String,
    private val port: String,
    val password: String
) {

    val uri: URI
        get() = URI.create("ws://$host${ if (port.isNotEmpty() && port.isNotBlank()) ":$port" else ""}")


    override fun toString(): String =
        "LavaNode{name ='$name' ,host='$host', port='$port', password='$password'}"

}
