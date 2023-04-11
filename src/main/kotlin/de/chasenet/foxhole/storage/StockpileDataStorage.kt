package de.chasenet.foxhole.storage

import de.chasenet.foxhole.domain.Stockpile
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

interface StockpileDataStorage {
    fun save(stockpile: Stockpile): Stockpile

    fun getAll(): List<Stockpile>

    fun get(code: String): Stockpile
    fun removeByCode(code: String)
}

class InMemoryStorage: StockpileDataStorage {
    private val storage = mutableMapOf<String, Stockpile>()
    override fun save(stockpile: Stockpile): Stockpile {
        storage[stockpile.code] = stockpile
        return stockpile
    }

    override fun getAll(): List<Stockpile> = storage.values.toList()

    override fun get(code: String): Stockpile = storage[code]!!

    override fun removeByCode(code: String) {
        storage.remove(code)
    }

}

val storageModule = module {
    singleOf(::InMemoryStorage) { bind<StockpileDataStorage>() }
}