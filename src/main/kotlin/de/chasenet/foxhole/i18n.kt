package de.chasenet.foxhole

import dev.kord.common.Locale

val DEFAULT_LOCALE = Locale.ENGLISH_GREAT_BRITAIN

object i18n {
    val INIT_COMMAND_DESCRIPTION = TranslationString.of("Adds a stockpile")
    val INIT_COMMAND_CODE = TranslationString.of("The stockpile code")
    val INIT_COMMAND_NAME = TranslationString.of("The stockpile name")
    val INIT_COMMAND_HEX = TranslationString.of("The hex the stockpile is in")
    val INIT_COMMAND_CITY = TranslationString.of("The city or region the stockpile is in")

    val REFRESH_BUTTON_LABEL = TranslationString.of("refresh")
    val DELETE_BUTTON_LABEL = TranslationString.of("delete")
}

data class TranslationString(
    private val _translations: Map<Locale, String>
) : Map<Locale, String> by _translations {
    val default = _translations[DEFAULT_LOCALE]!!

    val translations = _translations.toMutableMap()

    companion object {
        fun of(default: String, vararg entries: Pair<Locale, String>) =
            TranslationString(
                mapOf(
                    DEFAULT_LOCALE to default,
                    *entries.filter { it.first == DEFAULT_LOCALE }.toTypedArray()
                )
            )
    }
}