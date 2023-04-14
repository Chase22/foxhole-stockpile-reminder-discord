package de.chasenet.foxhole

import de.chasenet.foxhole.discord.CommandRegistry
import de.chasenet.foxhole.storage.ChannelStorageAdapter
import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.koin.core.context.startKoin
import org.koin.core.error.NoPropertyFileFoundException
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
        try { fileProperties() } catch (e: NoPropertyFileFoundException) { /* Ignore */ }
        environmentProperties()
        modules(
            kordModule,
        )
        createEagerInstances()
    }

    CoroutineScope(Dispatchers.IO).launch {
        startServer()
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
            channelId = getProperty<String>("fostorChannelId").toLong()
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
            reminderChannelId = getProperty<String>("fostorChannelId").toLong(),
        )
    }.withOptions {
        createdAtStart()
    }
}