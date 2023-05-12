package api.utils

import dev.minn.jda.ktx.jdabuilder.defaultShard
import main.main.Config
import main.main.RobertifyKt
import net.dv8tion.jda.api.sharding.ShardManager
import org.koin.dsl.module

val prodModule = module {
    single<ShardManager> { RobertifyKt.shardManager }
}

private val testShardManager by lazy {
    defaultShard(
        token = Config.BOT_TOKEN,
        enableCoroutines = false
    )
}

val testModule = module {
    single<ShardManager> { testShardManager }
}