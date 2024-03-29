package main.utils.database.mongodb

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import main.constants.database.RobertifyMongoDatabase
import main.main.Config

class MongoConnectionManager {

    companion object {
        private val CONNECTION_STRING = ConnectionString(RobertifyMongoDatabase.getConnectionString(Config.MONGO_DATABASE_NAME))
        private val CLIENT_SETTINGS = MongoClientSettings.builder()
            .applyConnectionString(CONNECTION_STRING)
            .build()
        private val client = MongoClients.create(CLIENT_SETTINGS)
        private var database: MongoDatabase? = null

        fun connect(databaseKt: RobertifyMongoDatabase): Companion = try {
            database = client.getDatabase(databaseKt.toString())
            this
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("There was no database with the name $databaseKt")
        }

        fun database(): MongoDatabase = database ?: throw NullPointerException("There is no connected database!")
    }
}