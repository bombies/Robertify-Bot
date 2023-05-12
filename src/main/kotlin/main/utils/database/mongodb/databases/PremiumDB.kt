package main.utils.database.mongodb.databases

import main.constants.database.MongoDatabase
import main.utils.database.mongodb.AbstractMongoDatabase

object PremiumDB : AbstractMongoDatabase(MongoDatabase.ROBERTIFY_DATABASE, MongoDatabase.ROBERTIFY_PREMIUM) {

}