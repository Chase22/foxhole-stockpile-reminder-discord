package de.chasenet.foxhole.discord

import de.chasenet.foxhole.domain.HEXES
import dev.kord.common.entity.Choice
import dev.kord.common.entity.optional.Optional
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.interaction.suggest
import dev.kord.core.event.interaction.AutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.rest.builder.interaction.string

suspend fun CommandRegistry.initPingCommand() {
    val command = kord.createGlobalChatInputCommand("ping", "Pings people") {
        string("hex", "The stockpile hex") {
            autocomplete = true
        }
    }

    registerCommandListener(command.id) {
        val response = interaction.deferPublicResponse()
        response.respond {
            content = "pong! ${interaction.command.strings["hex"]}"
        }
    }

    registerAutoCompleteListener(command.id, AutoCompleteInteractionCreateEvent::hexAutocompleteListener)
}

suspend fun AutoCompleteInteractionCreateEvent.hexAutocompleteListener() {
    val prefix = interaction.command.strings["hex"] ?: ""
    interaction.suggest(
        HEXES.partition { it.startsWith(prefix) }
            .let { it.first.sorted() + it.second.sorted() }
            .take(10)
            .map { Choice.StringChoice(it, Optional.invoke(), it) })
}

suspend fun ButtonInteractionCreateEvent.getCode(failureMessage: String): String? {
    return interaction.message.embeds.first().data.getFieldValue("code") ?: run {
        interaction.deferEphemeralResponse().respond {
            content = failureMessage
        }
        return null
    }
}

suspend fun CommandRegistry.initClearCommand() {
    val command = kord.createGlobalChatInputCommand("clear", "Clears the channel")

    registerCommandListener(command.id) {
        interaction.channel.messages.collect {
            it.delete()
        }
        interaction.deferEphemeralResponse().respond {
            content = "Channel cleared"
        }
    }
}