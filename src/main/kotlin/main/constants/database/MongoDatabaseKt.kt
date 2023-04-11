package main.constants.database

import main.constants.ENV
import main.main.ConfigKt

enum class MongoDatabaseKt(val str: String) {
    MAIN("Learning0"),

    ROBERTIFY_DATABASE(ConfigKt.get(ENV.MONGO_DATABASE_NAME)),

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
            "mongodb+srv://${ConfigKt.get(ENV.MONGO_USERNAME)}:${ConfigKt.get(ENV.MONGO_PASSWORD)}@${ConfigKt.get(ENV.MONGO_CLUSTER_NAME)}.${ENV.MONGO_HOSTNAME}/${db}?retryWrites=true&w=majority"
    }
}