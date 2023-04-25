package de.chasenet.foxhole.storage

import de.chasenet.foxhole.domain.Location
import de.chasenet.foxhole.domain.MessageId.Companion.toMessageId
import de.chasenet.foxhole.domain.Stockpile
import dev.kord.common.entity.DiscordEmbed
import dev.kord.common.entity.DiscordMessage
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import org.slf4j.LoggerFactory

class ChannelStorageAdapter(
    private val kord: Kord,
) {
    suspend fun getStockpiles(channelId: Snowflake): List<Stockpile> =
        getMessages(channelId).mapNotNull { message ->
            val embeds = message.embeds.firstOrNull() ?: return@mapNotNull null
            val fields = embeds.fields.value ?: return@mapNotNull null

            if (embeds.title.value?.lowercase() == "refresh") return@mapNotNull null

            val location = embeds.title.value!!.split(",").map(String::trim).let {
                Location(it.first(), it.last())
            }

            Stockpile(
                messageId = message.toMessageId(),
                code = fields.getFieldByName("code")!!,
                name = fields.getFieldByName("name")!!,
                location = location,
                expireTime = parseDiscordTimestamp(fields.getFieldByName("expires at")!!),
                refreshReminder = null
            )
        }

    suspend fun getStockpile(channelId: Snowflake, code: String) =
        getStockpiles(channelId).firstOrNull { it.code == code }

    private suspend fun getMessages(channelId: Snowflake): List<DiscordMessage> {
        LOGGER.debug("Getting messages for channel $channelId")
        return kord.rest.channel.getMessages(channelId).filter { it.author.id == kord.selfId }
    }

    suspend fun getReminders(channelId: Snowflake): Map<String, DiscordMessage> =
        getMessages(channelId).filter { it.embeds.firstOrNull()?.title?.value == "Refresh" }
            .associateBy { it.embeds.first().fields.value!!.getFieldByName("code")!! }

    suspend fun getReminder(channelId: Snowflake, code: String): DiscordMessage? = getReminders(channelId)[code]

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ChannelStorageAdapter::class.java)
    }
}

private fun parseDiscordTimestamp(timestamp: String) = Instant.fromEpochSeconds(timestamp.split(":")[1].toLong())
private fun List<DiscordEmbed.Field>.getFieldByName(name: String) = firstOrNull { it.name == name }?.value

suspend fun Guild.getStockpileChannel() = channels.filter { it.name == "stockpile-codes" }.first()