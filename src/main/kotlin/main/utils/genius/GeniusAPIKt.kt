package main.utils.genius

class GeniusAPIKt {

    fun search(query: String) =
        GeniusSongSearchKt(this, query)
}