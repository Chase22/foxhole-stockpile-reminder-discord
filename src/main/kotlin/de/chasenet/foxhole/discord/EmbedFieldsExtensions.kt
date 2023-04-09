package de.chasenet.foxhole.discord

import dev.kord.core.cache.data.EmbedData

fun EmbedData.getFieldValue(key: String): String? = fields.value?.find { it.name == key }?.value