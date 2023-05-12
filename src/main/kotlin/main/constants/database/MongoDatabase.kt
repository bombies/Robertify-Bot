package main.constants.database

import main.main.Config

enum class MongoDatabase(val str: String) {
    MAIN("Learning0"),

    ROBERTIFY_DATABASE(Config.MONGO_DATABASE_NAME),

    // Collections
    ROBERTIFY_BOT_INFO("main"),
    ROBERTIFY_GUILDS("guilds"),
    ROBERTIFY_PERMISSIONS("permissions"),
    ROBERTIFY_TEST("test"),
    ROBERTIFY_STATS("statistics"),
    ROBERTIFY_FAVOURITE_TRACKS("favouritetracks"),
    ROBERTIFY_PREMIUM("premium");

    override fun toString(): String = str

    companion object {
        fun getConnectionString(db: String) =
            "mongodb+srv://${Config.MONGO_USERNAME}:${Config.MONGO_PASSWORD}@${Config.MONGO_CLUSTER_NAME}.${Config.MONGO_HOSTNAME}/${db}?retryWrites=true&w=majority"
    }
}