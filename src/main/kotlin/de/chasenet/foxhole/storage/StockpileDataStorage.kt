package de.chasenet.foxhole.storage

import de.chasenet.foxhole.domain.Stockpile
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

interface StockpileDataStorage {
    fun save(stockpile: Stockpile): Stockpile

    fun getAll(): List<Stockpile>

    fun get(code: String): Stockpile
}

class InMemoryStorage: StockpileDataStorage {
    val storage = mutableMapOf<String, Stockpile>()
    override fun save(stockpile: Stockpile): Stockpile {
        storage[stockpile.code] = stockpile
        return stockpile
    }

    override fun getAll(): List<Stockpile> = storage.values.toList()

    override fun get(code: String): Stockpile = storage[code]!!

}

val storageModule = module {
    singleOf(::InMemoryStorage) { bind<StockpileDataStorage>() }
}