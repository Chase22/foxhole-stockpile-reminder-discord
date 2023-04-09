package de.chasenet.foxhole.discord

import de.chasenet.foxhole.domain.HEXES
import de.chasenet.foxhole.domain.Location
import de.chasenet.foxhole.domain.RESERVATION_EXPIRATION_TIME
import de.chasenet.foxhole.domain.Stockpile
import dev.kord.common.DiscordTimestampStyle
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Choice
import dev.kord.common.entity.optional.Optional
import dev.kord.common.toMessageFormat
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.interaction.suggest
import dev.kord.core.event.interaction.AutoCompleteInteractionCreateEvent
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import kotlinx.datetime.Clock

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
            required = false
        }
    }

    registerCommandListener(command.id) {
        val stockpile = this@initAddStockpileCommand.stockpileDataStorage.save(
            Stockpile(
                code = interaction.command.integers["code"].toString(),
                name = interaction.command.strings["name"]!!,
                location = Location(
                    hex = interaction.command.strings["hex"]!!,
                    city = interaction.command.strings["city"] ?: "",
                ),
                lastReset = Clock.System.now()
            )
        )
        val response = interaction.deferPublicResponse()
        response.respond {
            embed {
                title = "${stockpile.location.hex} ${stockpile.location.city}"
                field("name", true) { stockpile.name }
                field("code", true) { stockpile.code }
                field("expires at") {
                    stockpile.lastReset.plus(RESERVATION_EXPIRATION_TIME).toMessageFormat(DiscordTimestampStyle.LongDateTime)
                }
                field("in") {
                    stockpile.lastReset.plus(RESERVATION_EXPIRATION_TIME).toMessageFormat(DiscordTimestampStyle.RelativeTime)
                }
            }
            actionRow {
                interactionButton(ButtonStyle.Primary, "refreshButton") {
                    label = "refresh"
                }
            }
        }
    }

    registerAutoCompleteListener(command.id, AutoCompleteInteractionCreateEvent::hexAutocompleteListener)

    registerButtonListener("refreshButton") {
        val code = interaction.message.embeds.first().data.getFieldValue("code")
        if (code == null) {
            interaction.deferEphemeralResponse().respond {
                content = "Something went wrong trying to refresh the stockpile"
            }
            return@registerButtonListener
        }

        val stockpile = stockpileDataStorage.get(code)

        interaction.deferEphemeralResponse().respond {
            content = "Refreshed stockpile ${stockpile.name}"
        }
    }
}

suspend fun AutoCompleteInteractionCreateEvent.hexAutocompleteListener() {
    val prefix = interaction.command.strings["hex"] ?: ""
    interaction.suggest(
        HEXES.partition { it.startsWith(prefix) }
            .let { it.first.sorted() + it.second.sorted() }
            .take(10)
            .map { Choice.StringChoice(it, Optional.invoke(), it) })
}

suspend fun CommandRegistry.initListStockpilesCommand() {
    val command = kord.createGlobalChatInputCommand("list", "Lists all stockpiles")

    registerCommandListener(command.id) {
        interaction.deferPublicResponse().respond {
            content = stockpileDataStorage.getAll()
                .joinToString(System.lineSeparator()) { "${it.name}: ${it.location.hex} `${it.code}`" }
        }
    }
}