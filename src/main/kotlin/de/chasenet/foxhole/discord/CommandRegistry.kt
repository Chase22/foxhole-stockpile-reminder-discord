package de.chasenet.foxhole.discord

import de.chasenet.foxhole.storage.ChannelStorageAdapter
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.Event
import dev.kord.core.event.interaction.AutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory

class EventListenerMap<T : Event> : HashMap<Snowflake, suspend T.() -> Unit>()

class CommandRegistry(
    val kord: Kord,
    val clock: Clock,
    val storageAdapter: ChannelStorageAdapter,
    val meterRegistry: MeterRegistry
) {
    private val logger = LoggerFactory.getLogger(CommandRegistry::class.java)

    private val commandListeners: EventListenerMap<ChatInputCommandInteractionCreateEvent> = EventListenerMap()
    private val buttonListeners: MutableMap<String, suspend ButtonInteractionCreateEvent.() -> Unit> = mutableMapOf()

    fun registerCommandListener(id: Snowflake, listener: suspend ChatInputCommandInteractionCreateEvent.() -> Unit) {
        commandListeners[id] = listener
    }

    fun registerButtonListener(id: String, listener: suspend ButtonInteractionCreateEvent.() -> Unit) {
        buttonListeners[id] = listener
    }

    private val commands: List<suspend CommandRegistry.() -> Unit> = listOf(
        CommandRegistry::initAddStockpileCommand,
        CommandRegistry::initEditCommand,
    )

    init {
        logger.info("Initializing Command registry")
        CoroutineScope(Dispatchers.IO).launch {
            commands.forEach { it() }

            kord.on<ChatInputCommandInteractionCreateEvent> {
                meterRegistry.counter("command_invoked", "command", interaction.command.rootName)
                commandListeners[interaction.command.rootId]?.invoke(this)
            }
            kord.on<AutoCompleteInteractionCreateEvent> {
                when(interaction.command.options.entries.first { it.value.focused }.key) {
                    COMMAND_HEX_FIELD -> hexAutocompleteListener()
                    COMMAND_CODE_FIELD -> codeAutocompleteListener()
                }
            }

            kord.on<ButtonInteractionCreateEvent> {
                meterRegistry.counter("button_interaction", "button", interaction.componentId)
                buttonListeners[interaction.componentId]?.invoke(this)
            }
        }
    }
}