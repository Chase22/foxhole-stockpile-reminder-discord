package de.chasenet.foxhole.discord

import de.chasenet.foxhole.domain.Location
import de.chasenet.foxhole.i18n
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.modify.embed

suspend fun ButtonInteractionCreateEvent.getCode(failureMessage: String): String? {
    return interaction.message.embeds.first().data.getFieldValue("code") ?: run {
        interaction.deferEphemeralResponse().respond {
            content = failureMessage
        }
        return null
    }
}

suspend fun CommandRegistry.initEditCommand() {
    val command = kord.createGlobalChatInputCommand("edit", i18n.EDIT_COMMAND_DESCRIPTION.default) {
        integer(COMMAND_CODE_FIELD, i18n.INIT_COMMAND_CODE.default) {
            descriptionLocalizations = i18n.INIT_COMMAND_CODE.translations.toMutableMap()
            required = true
            maxValue = 999999
            minValue = 100000
            autocomplete = true
        }
        string(COMMAND_NAME_FIELD, i18n.INIT_COMMAND_NAME.default) {
            descriptionLocalizations = i18n.INIT_COMMAND_NAME.translations.toMutableMap()
            required = false
            minLength = 3

        }
        string(COMMAND_HEX_FIELD, i18n.INIT_COMMAND_HEX.default) {
            descriptionLocalizations = i18n.INIT_COMMAND_HEX.translations.toMutableMap()
            required = false
            autocomplete = true
        }
        string(COMMAND_CITY_FIELD, i18n.INIT_COMMAND_CITY.default) {
            descriptionLocalizations = i18n.INIT_COMMAND_CITY.translations.toMutableMap()
            required = false
        }
    }

    registerCommandListener(command.id) {
        val stockpile = storageAdapter.getStockpile(interaction.channelId, interaction.command.integers[COMMAND_CODE_FIELD].toString())
        if (stockpile == null) {
            interaction.deferEphemeralResponse().respond {
                content = "Stockpile to delete was not found"
            }
            return@registerCommandListener
        }

        stockpile.copy(
            name = interaction.command.strings[COMMAND_NAME_FIELD] ?: stockpile.name,
            location = Location(
                hex = interaction.command.strings[COMMAND_HEX_FIELD] ?: stockpile.location.hex,
                city = interaction.command.strings[COMMAND_CITY_FIELD] ?: stockpile.location.city,
            )
        ).also {
            kord.rest.channel.editMessage(stockpile.messageId.channelId, stockpile.messageId.messageId) {
                embed { embedStockpile(it) }
            }
            interaction.deferEphemeralResponse().respond {
                content = "Stockpile updated"
            }
        }
    }
}