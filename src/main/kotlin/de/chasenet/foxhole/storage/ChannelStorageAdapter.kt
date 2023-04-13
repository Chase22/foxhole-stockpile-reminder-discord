package de.chasenet.foxhole.storage

import de.chasenet.foxhole.domain.Location
import de.chasenet.foxhole.domain.Stockpile
import dev.kord.common.entity.DiscordEmbed
import dev.kord.common.entity.DiscordMessage
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import kotlinx.datetime.Instant

class ChannelStorageAdapter(
    private val kord: Kord,
    private val channelId: Long
) {
    suspend fun getStockpiles(): List<Stockpile> =
        getMessages().filter { it.author.id == kord.selfId }
            .mapNotNull { message ->
                val embeds = message.embeds.firstOrNull() ?: return@mapNotNull null
                val fields = embeds.fields.value ?: return@mapNotNull null

                if (embeds.title.value?.lowercase() == "refresh") return@mapNotNull null

                val location = embeds.title.value!!.split(",").map(String::trim).let {
                    Location(it.first(), it.last())
                }

                Stockpile(
                    code = fields.getFieldByName("code")!!,
                    name = fields.getFieldByName("name")!!,
                    location = location,
                    expireTime = parseDiscordTimestamp(fields.getFieldByName("expires at")!!),
                    refreshReminder = null
                )
            }

    suspend fun getStockpile(code: String) = getStockpiles().firstOrNull { it.code == code }

    private suspend fun getMessages() = kord.rest.channel.getMessages(Snowflake(channelId))

    suspend fun getReminders(): Map<String, DiscordMessage> =
        getMessages().filter { it.embeds.firstOrNull()?.title?.value == "Refresh" }
            .associateBy { it.embeds.first().fields.value!!.getFieldByName("code")!! }

    suspend fun getReminder(code: String): DiscordMessage? = getReminders()[code]
}

private fun parseDiscordTimestamp(timestamp: String) = Instant.fromEpochSeconds(timestamp.split(":")[1].toLong())
private fun List<DiscordEmbed.Field>.getFieldByName(name: String) = firstOrNull { it.name == name }?.value