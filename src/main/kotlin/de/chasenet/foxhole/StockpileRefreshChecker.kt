package de.chasenet.foxhole

import de.chasenet.foxhole.storage.StockpileDataStorage
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes
import de.chasenet.foxhole.domain.MessageId.Companion.toMessageId

class StockpileRefreshChecker(
    private val stockpileDataStorage: StockpileDataStorage,
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
                stockpileDataStorage.getAll().filter {
                    (it.expireTime - clock.now()).inWholeHours < 4 && it.refreshReminder == null
                }.forEach {
                    val refreshMessage = kord.rest.channel.createMessage(Snowflake(reminderChannelId)) {
                        content =
                            "Stockpile ${it.name} in ${it.location.hex}, ${it.location.city} needs to be refreshed"
                    }
                    stockpileDataStorage.save(it.copy(refreshReminder = refreshMessage.toMessageId()))
                }
                delay(1.minutes)
            }
        }
    }
}