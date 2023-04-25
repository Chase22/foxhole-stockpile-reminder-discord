package de.chasenet.foxhole.discord

import de.chasenet.foxhole.domain.HEXES
import de.chasenet.foxhole.storage.ChannelStorageAdapter
import dev.kord.common.entity.Choice
import dev.kord.common.entity.optional.Optional
import dev.kord.core.behavior.interaction.suggest
import dev.kord.core.event.interaction.AutoCompleteInteractionCreateEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

internal object AutocompleteDependencies : KoinComponent {
    internal val storageAdapter: ChannelStorageAdapter = get()
}


suspend fun AutoCompleteInteractionCreateEvent.hexAutocompleteListener() {
    val prefix = interaction.command.strings["hex"] ?: ""
    interaction.suggest(
        HEXES.partition { it.startsWith(prefix) }
            .let { it.first.sorted() + it.second.sorted() }
            .take(10)
            .map { Choice.StringChoice(it, Optional.invoke(), it) })
}

suspend fun AutoCompleteInteractionCreateEvent.codeAutocompleteListener() {
    val prefix = interaction.command.strings["code"] ?: ""
    val codes = AutocompleteDependencies.storageAdapter.getStockpiles(interaction.channelId).map { it.code }

    interaction.suggest(
        codes.partition { it.startsWith(prefix) }
            .let { it.first.sorted() + it.second.sorted() }
            .take(10)
            .map { Choice.StringChoice(it, Optional.invoke(), it) })
}
