package de.chasenet.foxhole.discord

import de.chasenet.foxhole.storage.StockpileDataStorage
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.Event
import dev.kord.core.event.interaction.AutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory

class EventListenerMap<T: Event>: HashMap<Snowflake, suspend T.() -> Unit>()

class CommandRegistry(val kord: Kord, val stockpileDataStorage: StockpileDataStorage, val clock: Clock) {
    private val logger = LoggerFactory.getLogger(CommandRegistry::class.java)

    private val commandListeners: EventListenerMap<ChatInputCommandInteractionCreateEvent> = EventListenerMap()
    private val autocompleteListeners: EventListenerMap<AutoCompleteInteractionCreateEvent> = EventListenerMap()
    private val buttonListeners: MutableMap<String, suspend ButtonInteractionCreateEvent.() -> Unit> = mutableMapOf()

    fun registerCommandListener(id: Snowflake, listener: suspend ChatInputCommandInteractionCreateEvent.() -> Unit) {
        commandListeners[id] = listener
    }

    fun registerAutoCompleteListener(id: Snowflake, listener: suspend AutoCompleteInteractionCreateEvent.() -> Unit) {
        autocompleteListeners[id] = listener
    }

    fun registerButtonListener(id: String, listener: suspend ButtonInteractionCreateEvent.() -> Unit) {
        buttonListeners[id] = listener
    }

    private val commands: List<suspend CommandRegistry.() -> Unit> = listOf(
        CommandRegistry::initPingCommand,
        CommandRegistry::initAddStockpileCommand,
        CommandRegistry::initListStockpilesCommand,
    )

    init {
        logger.info("Initializing Command registry")
        CoroutineScope(Dispatchers.IO).launch {
            commands.forEach { it() }

            kord.on<ChatInputCommandInteractionCreateEvent> {
                commandListeners[interaction.command.rootId]?.invoke(this)
            }
            kord.on<AutoCompleteInteractionCreateEvent> {
                autocompleteListeners[interaction.command.rootId]?.invoke(this)
            }

            kord.on<ButtonInteractionCreateEvent> {
                buttonListeners[interaction.componentId]?.invoke(this)
            }
        }
    }
}