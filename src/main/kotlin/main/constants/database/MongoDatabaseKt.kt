package main.constants.database

import main.constants.ENVKt
import main.main.ConfigKt

enum class MongoDatabaseKt(val str: String) {
    MAIN("Learning0"),

    ROBERTIFY_DATABASE(ConfigKt.get(ENVKt.MONGO_DATABASE_NAME)),

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
            "mongodb+srv://${ConfigKt[ENVKt.MONGO_USERNAME]}:${ConfigKt[ENVKt.MONGO_PASSWORD]}@${ConfigKt[ENVKt.MONGO_CLUSTER_NAME]}.${ConfigKt[ENVKt.MONGO_HOSTNAME]}/${db}?retryWrites=true&w=majority"
    }
}