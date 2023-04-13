package de.chasenet.foxhole.domain

import dev.kord.common.entity.DiscordMessage
import dev.kord.common.entity.Snowflake
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

data class Stockpile(
    val messageId: MessageId,
    val code: String,
    val name: String,
    val location: Location,
    val expireTime: Instant,
    val refreshReminder: MessageId? = null
)

data class MessageId(
    val channelId: Snowflake,
    val messageId: Snowflake
) {
    companion object {
        fun DiscordMessage.toMessageId() = MessageId(channelId, id)
    }
}

data class Location(
    val hex: String,
    val city: String
)

val RESERVATION_EXPIRATION_TIME = 2.days + 4.hours

val HEXES = object{}::class.java.classLoader.getResource("hexes.txt")!!.readText().lines()