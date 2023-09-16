package main.utils.json.requestchannel

enum class RequestChannelButtonId(private val str: String) {
    IDENTIFIER("dedicated"),
    PREVIOUS("${IDENTIFIER}previous"),
    REWIND("${IDENTIFIER}rewind"),
    PLAY_AND_PAUSE("${IDENTIFIER}pnp"),
    STOP("${IDENTIFIER}stop"),
    SKIP("${IDENTIFIER}end"),
    LOOP("${IDENTIFIER}loop"),
    SHUFFLE("${IDENTIFIER}shuffle"),
    DISCONNECT("${IDENTIFIER}disconnect"),
    FAVOURITE("${IDENTIFIER}favourite"),
    FILTERS("${IDENTIFIER}filters");

    override fun toString(): String = str
}