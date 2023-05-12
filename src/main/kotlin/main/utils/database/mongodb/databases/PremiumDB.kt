package main.utils.database.mongodb.databases

import main.constants.database.RobertifyMongoDatabase
import main.utils.database.mongodb.AbstractMongoDatabase

object PremiumDB : AbstractMongoDatabase(RobertifyMongoDatabase.ROBERTIFY_DATABASE, RobertifyMongoDatabase.ROBERTIFY_PREMIUM) {

}