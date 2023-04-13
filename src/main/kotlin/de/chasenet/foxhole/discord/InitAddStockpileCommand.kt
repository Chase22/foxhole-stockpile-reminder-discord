package de.chasenet.foxhole.discord

import de.chasenet.foxhole.domain.Location
import de.chasenet.foxhole.domain.RESERVATION_EXPIRATION_TIME
import de.chasenet.foxhole.domain.Stockpile
import dev.kord.common.DiscordTimestampStyle
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.toMessageFormat
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.AutoCompleteInteractionCreateEvent
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed

private fun EmbedBuilder.embedStockpile(stockpile: Stockpile) {
    title = "${stockpile.location.hex}, ${stockpile.location.city}"
    field("name", true) { stockpile.name }
    field("code", true) { stockpile.code }
    field("expires at") {
        stockpile.expireTime.toMessageFormat(DiscordTimestampStyle.LongDateTime)
    }
    field("in") {
        stockpile.expireTime.toMessageFormat(DiscordTimestampStyle.RelativeTime)
    }
}

private const val REFRESH_BUTTON_CUSTOM_ID = "refreshButton"
private const val DELETE_BUTTON_CUSTOM_ID = "deleteButton"

suspend fun CommandRegistry.initAddStockpileCommand() {

    val command = kord.createGlobalChatInputCommand("add", "Adds a stockpile") {
        integer("code", "The stockpile code") {
            required = true
            maxValue = 999999
            minValue = 100000
        }
        string("name", "The stockpile name") {
            required = true
            minLength = 3

        }
        string("hex", "The hex the stockpile is in") {
            required = true
            autocomplete = true
        }
        string("city", "The city or region the stockpile is in") {
            required = true
        }
    }

    registerCommandListener(command.id) {
        val stockpile = Stockpile(
            code = interaction.command.integers["code"].toString(),
            name = interaction.command.strings["name"]!!,
            location = Location(
                hex = interaction.command.strings["hex"]!!,
                city = interaction.command.strings["city"]!!,
            ),
            expireTime = clock.now().plus(RESERVATION_EXPIRATION_TIME)
        )

        interaction.deferPublicResponse().respond {
            embed {
                embedStockpile(stockpile)
            }
            actionRow {
                interactionButton(ButtonStyle.Primary, REFRESH_BUTTON_CUSTOM_ID) {
                    label = "refresh"
                }
                interactionButton(ButtonStyle.Danger, DELETE_BUTTON_CUSTOM_ID) {
                    label = "delete"
                }
            }
        }
    }

    registerAutoCompleteListener(command.id, AutoCompleteInteractionCreateEvent::hexAutocompleteListener)

    registerButtonListener(REFRESH_BUTTON_CUSTOM_ID) {
        getCode("Something went wrong trying to refresh the stockpile")?.also { stockpileCode ->
            val stockpile = storageAdapter.getStockpile(stockpileCode)!!.copy(
                expireTime = clock.now().plus(RESERVATION_EXPIRATION_TIME)
            )

            storageAdapter.getReminder(stockpileCode)
                ?.let { reminder ->
                    kord.rest.channel.deleteMessage(reminder.channelId, reminder.id, "Stockpile was refreshed")
                }

            interaction.message.edit {
                embed {
                    embedStockpile(stockpile)
                }
            }
            interaction.deferEphemeralResponse().respond {
                content = "Refreshed stockpile ${stockpile.name}"
            }
        }
    }

    registerButtonListener(DELETE_BUTTON_CUSTOM_ID) {
        getCode("Something went wrong trying to delete the stockpile")?.also {
            val stockpile = storageAdapter.getStockpile(it)

            if (stockpile == null) {
                interaction.deferEphemeralResponse().respond {
                    embed {
                        title = "Stockpile to delete was not found"
                    }
                }
                return@also
            }

            storageAdapter.getReminder(it)
                ?.let { reminder ->
                    kord.rest.channel.deleteMessage(
                        reminder.channelId,
                        reminder.id,
                        "Stockpile was deleted"
                    )
                }

            interaction.message.delete("Stockpile removed by ${interaction.data.user.value!!.username}")
            interaction.deferPublicResponse()
                .respond { content = "${interaction.data.user.value!!.username} removed ${stockpile.name}" }
        }
    }
}