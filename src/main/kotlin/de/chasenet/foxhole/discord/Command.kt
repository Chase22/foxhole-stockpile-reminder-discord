package de.chasenet.foxhole.discord

import de.chasenet.foxhole.domain.HEXES
import de.chasenet.foxhole.i18n
import dev.kord.common.entity.Choice
import dev.kord.common.entity.optional.Optional
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.interaction.suggest
import dev.kord.core.event.interaction.AutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.json.request.BulkDeleteRequest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

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

suspend fun CommandRegistry.initEditCommand() {
    kord.createGlobalChatInputCommand("edit", i18n.EDIT_COMMAND_DESCRIPTION.default) {
        integer(COMMAND_CODE_FIELD, i18n.INIT_COMMAND_CODE.default) {
            descriptionLocalizations = i18n.INIT_COMMAND_CODE.translations.toMutableMap()
            required = true
            maxValue = 999999
            minValue = 100000
        }
        string(COMMAND_NAME_FIELD, i18n.INIT_COMMAND_NAME.default) {
            descriptionLocalizations = i18n.INIT_COMMAND_NAME.translations.toMutableMap()
            required = true
            minLength = 3

        }
        string(COMMAND_HEX_FIELD, i18n.INIT_COMMAND_HEX.default) {
            descriptionLocalizations = i18n.INIT_COMMAND_HEX.translations.toMutableMap()
            required = true
            autocomplete = true
        }
        string(COMMAND_CITY_FIELD, i18n.INIT_COMMAND_CITY.default) {
            descriptionLocalizations = i18n.INIT_COMMAND_CITY.translations.toMutableMap()
            required = true
        }
    }
}

suspend fun CommandRegistry.initListCommand() {
    val command = kord.createGlobalChatInputCommand("list", "Lists all stockpiles")
    registerCommandListener(command.id) {
        interaction.deferEphemeralResponse().respond {
            content = storageAdapter.getStockpiles().toString()
        }
    }
}

suspend fun CommandRegistry.initClearCommand() {
    val command = kord.createGlobalChatInputCommand("clear", "Clears the channel")

    registerCommandListener(command.id) {
        kord.rest.channel.bulkDelete(interaction.channelId,
            BulkDeleteRequest(interaction.channel.messages.map { it.id }.toList())
        )

        interaction.deferEphemeralResponse().respond {
            content = "Channel cleared"
        }
    }
}