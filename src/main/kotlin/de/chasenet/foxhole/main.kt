package de.chasenet.foxhole

import de.chasenet.foxhole.discord.CommandRegistry
import de.chasenet.foxhole.storage.ChannelStorageAdapter
import dev.kord.common.entity.*
import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
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

        CoroutineScope(Dispatchers.IO).launch {
            startServer(koin.get())
        }
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
        PrometheusMeterRegistry(PrometheusConfig.DEFAULT).also {
            ProcessorMetrics().bindTo(it)
            LogbackMetrics().bindTo(it)
            JvmInfoMetrics().bindTo(it)
            JvmMemoryMetrics().bindTo(it)
        }
    }.bind(MeterRegistry::class)

    single {
        Clock.System
    }.bind(Clock::class)

    single {
        ChannelStorageAdapter(
            kord = get(),
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
        )
    }.withOptions {
        createdAtStart()
    }
}