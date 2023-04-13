import de.chasenet.foxhole.StockpileRefreshChecker
import de.chasenet.foxhole.discord.CommandRegistry
import de.chasenet.foxhole.storage.ChannelStorageAdapter
import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.environmentProperties
import org.koin.fileProperties

@OptIn(PrivilegedIntent::class)
suspend fun main() {
    val application = startKoin {
        fileProperties()
        environmentProperties()
        modules(
            kordModule,
        )
        createEagerInstances()
    }

    application.koin.get<Kord>().login {
        // we need to specify this to receive the content of messages
        intents += Intent.MessageContent
    }
}

val kordModule = module {
    single {
        runBlocking {
            Kord(getProperty("kordToken"))
        }
    }

    single {
        Clock.System
    }.bind(Clock::class)

    single {
        ChannelStorageAdapter(
            kord = get(),
            channelId = getProperty<String>("reminderChannelId").toLong()
        )
    }

    singleOf(::CommandRegistry).withOptions {
        createdAtStart()
    }

    single {
        StockpileRefreshChecker(
            channelStorageAdapter = get(),
            clock = get(),
            kord = get(),
            reminderChannelId = getProperty<String>("reminderChannelId").toLong(),
        )
    }.withOptions {
        createdAtStart()
    }
}