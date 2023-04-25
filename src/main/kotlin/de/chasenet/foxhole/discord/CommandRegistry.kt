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
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory

class EventListenerMap<T : Event> : HashMap<Snowflake, suspend T.() -> Unit>()

class CommandRegistry(
    val kord: Kord,
    val clock: Clock,
    val storageAdapter: ChannelStorageAdapter,
    private val meterRegistry: MeterRegistry
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
                meterRegistry.timer("command.invoked", "command", interaction.command.rootName).recordSuspend {
                    commandListeners[interaction.command.rootId]?.invoke(this)
                }
            }
            kord.on<AutoCompleteInteractionCreateEvent> {
                interaction.command.options.entries.first { it.value.focused }.key.let {
                    meterRegistry.timer("autocomplete.requested", "focusedKey", it).recordSuspend {
                        when(it) {
                            COMMAND_HEX_FIELD -> hexAutocompleteListener()
                            COMMAND_CODE_FIELD -> codeAutocompleteListener()
                        }
                    }
                }
            }

            kord.on<ButtonInteractionCreateEvent> {
                meterRegistry.counter("button_interaction", "button", interaction.componentId)
                buttonListeners[interaction.componentId]?.invoke(this)
            }
        }
    }
}

suspend fun Timer.recordSuspend(block: suspend () -> Unit) {
    val context = currentCoroutineContext()
    record(Runnable {
        CoroutineScope(context).launch {
            block()
        }
    })
}