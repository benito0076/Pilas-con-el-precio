package co.pilas.precio.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout

data class LiveResult(
    val products: List<LiveProduct>,
    /** Nombres de las tiendas que respondieron. */
    val answered: List<String>,
    /** Nombres de las tiendas que fallaron (sin red, bloqueo, formato inesperado...). */
    val failed: List<String>,
)

/** Consulta en paralelo todas las fuentes de precios en vivo. */
class LiveSearch(private val sources: List<PriceSource>) {

    suspend fun search(query: String): LiveResult = coroutineScope {
        val outcomes = sources.map { src ->
            src to async {
                runCatching { withTimeout(12_000) { src.search(query) } }
            }
        }.map { (src, deferred) -> src to deferred.await() }

        val offers = outcomes.flatMap { (_, r) -> r.getOrNull().orEmpty() }
        LiveResult(
            products = OfferMerger.merge(offers),
            answered = outcomes.filter { it.second.isSuccess }.map { it.first.store.name },
            failed = outcomes.filter { it.second.isFailure }.map { it.first.store.name },
        )
    }

    companion object {
        fun default(): LiveSearch {
            fun store(id: String) = SeedData.stores.first { it.id == id }
            return LiveSearch(
                listOf(
                    VtexSource(store("exito"), "www.exito.com"),
                    VtexSource(store("carulla"), "www.carulla.com"),
                    VtexSource(store("jumbo"), "www.jumbocolombia.com"),
                    VtexSource(store("olimpica"), "www.olimpica.com"),
                ),
            )
        }
    }
}
