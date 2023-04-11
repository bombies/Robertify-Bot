package main.utils.database.mongodb.databases

import main.constants.database.MongoDatabaseKt
import main.utils.database.mongodb.AbstractMongoDatabaseKt

class PremiumDBKt private constructor() : AbstractMongoDatabaseKt(MongoDatabaseKt.ROBERTIFY_DATABASE, MongoDatabaseKt.ROBERTIFY_PREMIUM) {

    companion object {
        private var INSTANCE: PremiumDBKt? = null

        fun ins(): PremiumDBKt {
            if (INSTANCE == null)
                INSTANCE = PremiumDBKt()
            return INSTANCE!!
        }
    }

}