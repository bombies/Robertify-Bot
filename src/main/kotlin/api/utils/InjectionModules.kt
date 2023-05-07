package api.utils

import dev.minn.jda.ktx.jdabuilder.defaultShard
import main.main.ConfigKt
import main.main.RobertifyKt
import net.dv8tion.jda.api.sharding.ShardManager
import org.koin.dsl.module

val prodModule = module {
    single<ShardManager> { RobertifyKt.shardManager }
}

val testShardManager by lazy {
    defaultShard(
        token = ConfigKt.BOT_TOKEN,
        enableCoroutines = false
    )
}

val testModule = module {
    single<ShardManager> { testShardManager }
}