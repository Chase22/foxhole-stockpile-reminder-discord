package de.chasenet.foxhole

import de.chasenet.foxhole.storage.ChannelStorageAdapter
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class StockpileRefreshChecker(
    private val channelStorageAdapter: ChannelStorageAdapter,
    private val clock: Clock,
    private val kord: Kord,
    private val reminderChannelId: Long
) : CoroutineScope {
    private val logger = LoggerFactory.getLogger(StockpileRefreshChecker::class.java)
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    init {
        launch {
            logger.info("Checker started")
            while (this.isActive) {
                val reminders = channelStorageAdapter.getReminders()

                channelStorageAdapter.getStockpiles().filter {
                    (it.expireTime.minus(2.days).minus(2.hours) - clock.now()).inWholeHours < 4
                }.forEach {
                    if (reminders.containsKey(it.code)) return@forEach
                    kord.rest.channel.createMessage(Snowflake(reminderChannelId)) {
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
                delay(1.minutes)
            }
        }
    }
}