package de.chasenet.foxhole.discord

import de.chasenet.foxhole.domain.Location
import de.chasenet.foxhole.domain.MessageId
import de.chasenet.foxhole.domain.RESERVATION_EXPIRATION_TIME
import de.chasenet.foxhole.domain.Stockpile
import de.chasenet.foxhole.i18n
import dev.kord.common.DiscordTimestampStyle
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.common.toMessageFormat
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed

private const val INIT_COMMAND_NAME = "add"

const val COMMAND_CODE_FIELD = "code"
const val COMMAND_NAME_FIELD = "name"
const val COMMAND_HEX_FIELD = "hex"
const val COMMAND_CITY_FIELD = "city"

private const val REFRESH_BUTTON_CUSTOM_ID = "refreshButton"
private const val DELETE_BUTTON_CUSTOM_ID = "deleteButton"

fun EmbedBuilder.embedStockpile(stockpile: Stockpile) {
    title = "${stockpile.location.hex}, ${stockpile.location.city}"
    field(COMMAND_NAME_FIELD, true) { stockpile.name }
    field(COMMAND_CODE_FIELD, true) { stockpile.code }
    field("expires at") {
        stockpile.expireTime.toMessageFormat(DiscordTimestampStyle.LongDateTime)
    }
    field("in") {
        stockpile.expireTime.toMessageFormat(DiscordTimestampStyle.RelativeTime)
    }
}

suspend fun CommandRegistry.initAddStockpileCommand() {
    val command = kord.createGlobalChatInputCommand(INIT_COMMAND_NAME, i18n.INIT_COMMAND_DESCRIPTION.default) {
        descriptionLocalizations = i18n.INIT_COMMAND_DESCRIPTION.translations

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

    registerCommandListener(command.id) {
        val stockpile = Stockpile(
            messageId = MessageId(Snowflake(0), Snowflake(0)),
            code = interaction.command.integers[COMMAND_CODE_FIELD].toString(),
            name = interaction.command.strings[COMMAND_NAME_FIELD]!!,
            location = Location(
                hex = interaction.command.strings[COMMAND_HEX_FIELD]!!,
                city = interaction.command.strings[COMMAND_CITY_FIELD]!!,
            ),
            expireTime = clock.now().plus(RESERVATION_EXPIRATION_TIME)
        )

        if (storageAdapter.getStockpile(interaction.channelId, stockpile.code) != null) {
            interaction.deferEphemeralResponse().respond {
                content = "There's already a stockpile with code ${stockpile.code}"
            }
        }

        interaction.deferPublicResponse().respond {
            embed {
                embedStockpile(stockpile)
            }
            actionRow {
                interactionButton(ButtonStyle.Primary, REFRESH_BUTTON_CUSTOM_ID) {
                    label = i18n.REFRESH_BUTTON_LABEL.default
                }
                interactionButton(ButtonStyle.Danger, DELETE_BUTTON_CUSTOM_ID) {
                    label = i18n.DELETE_BUTTON_LABEL.default
                }
            }
        }
    }

    registerButtonListener(REFRESH_BUTTON_CUSTOM_ID) {
        getCode("Something went wrong trying to refresh the stockpile")?.also { stockpileCode ->
            val stockpile = storageAdapter.getStockpile(interaction.channelId, stockpileCode)!!.copy(
                expireTime = clock.now().plus(RESERVATION_EXPIRATION_TIME)
            )

            println("Getting reminder")
            storageAdapter.getReminder(interaction.channelId, stockpileCode)
                ?.let { reminder ->
                    kord.rest.channel.deleteMessage(reminder.channelId, reminder.id, "Stockpile was refreshed")
                }

            println("Editing message reminder")
            interaction.message.edit {
                embed {
                    embedStockpile(stockpile)
                }
            }
            println("Response")
            interaction.deferEphemeralResponse().respond {
                content = "Refreshed stockpile ${stockpile.name}"
            }
        }
    }

    registerButtonListener(DELETE_BUTTON_CUSTOM_ID) {
        getCode("Something went wrong trying to delete the stockpile")?.also {
            val stockpile = storageAdapter.getStockpile(interaction.channelId, it)

            if (stockpile == null) {
                interaction.deferEphemeralResponse().respond {
                    content = "Stockpile to delete was not found"
                }
                return@also
            }

            storageAdapter.getReminder(interaction.channelId, it)
                ?.let { reminder ->
                    kord.rest.channel.deleteMessage(
                        reminder.channelId,
                        reminder.id,
                        "Stockpile was deleted"
                    )
                }

            interaction.message.delete("Stockpile removed by ${interaction.data.user.value!!.username}")
            interaction.deferPublicResponse()
                .respond {
                    content =
                        "${interaction.data.user.value!!.username} removed Stockpile ${stockpile.name} in ${stockpile.location.city}, ${stockpile.location.hex}"
                }
        }
    }
}