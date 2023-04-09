import de.chasenet.foxhole.discord.CommandRegistry
import de.chasenet.foxhole.storage.storageModule
import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

@OptIn(PrivilegedIntent::class)
suspend fun main() {
    val application = startKoin {
        modules(
            kordModule,
            storageModule
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
            Kord("MTA5MzQyODAyMTk3MDYwODE5OQ.Gc9fiT.06T5cR1ZZLaKXAuMSaqeIefMhe2PJNN1XpzCOM")
        }
    }

    singleOf(::CommandRegistry).withOptions {
        createdAtStart()
    }
}