package main.utils.genius

class GeniusAPI {

    fun search(query: String) =
        GeniusSongSearch(this, query)
}