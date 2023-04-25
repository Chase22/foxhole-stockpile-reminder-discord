package de.chasenet.foxhole

import de.chasenet.foxhole.storage.ChannelStorageAdapter
import de.chasenet.foxhole.storage.getStockpileChannel
import dev.kord.core.Kord
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes

class StockpileRefreshChecker(
    private val channelStorageAdapter: ChannelStorageAdapter,
    private val clock: Clock,
    private val kord: Kord,
) : CoroutineScope {
    private val logger = LoggerFactory.getLogger(StockpileRefreshChecker::class.java)
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    init {
        launch {
            logger.info("Checker started")
            while (this.isActive) {
                kord.guilds.onEach {
                    val channelId = it.getStockpileChannel().id
                    val reminders = channelStorageAdapter.getReminders(channelId)

                    channelStorageAdapter.getStockpiles(channelId).filter {
                        (it.expireTime - clock.now()).inWholeHours < 4
                    }.forEach {
                        if (reminders.containsKey(it.code)) return@forEach
                        kord.rest.channel.createMessage(channelId) {
                            embed {
                                title = "Refresh"
                                description =
                                    "A stockpile needs to be refreshed. Just renew the reservation in foxhole and click the refresh button under the stockpile"
                                field("name", false) { it.name }
                                field("code", true) { it.code }
                                field("hex", false) { it.location.hex }
                                field("city", true) { it.location.city }
                            }
                        }
                    }
                }

                delay(1.minutes)
            }
        }
    }
}