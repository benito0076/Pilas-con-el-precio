package co.pilas.precio.data

import co.pilas.precio.model.Product
import co.pilas.precio.model.Store

/** Resultado de comprar toda la lista (o lo que se pueda) en una sola tienda. */
data class StoreBasket(
    val store: Store,
    val total: Int,
    val missing: List<Product>,
) {
    val complete: Boolean get() = missing.isEmpty()
}

data class BasketComparison(
    /** Completas primero (de menor a mayor total); luego las incompletas. */
    val byStore: List<StoreBasket>,
    /** Total comprando cada producto en su tienda más barata. */
    val splitTotal: Int,
    /** Productos de la lista sin precio en ninguna tienda. */
    val unavailable: List<Product>,
) {
    val bestSingleStore: StoreBasket? get() = byStore.firstOrNull { it.complete }

    /** Ahorro de repartir la compra frente a la mejor tienda única completa. */
    val splitSavings: Int
        get() = bestSingleStore?.let { (it.total - splitTotal).coerceAtLeast(0) } ?: 0
}

object BasketCalculator {

    /** @param items producto -> cantidad de unidades a comprar (>= 1). */
    fun compare(items: Map<Product, Int>, repo: PriceRepository, stores: List<Store>): BasketComparison {
        val pricesByProduct = items.keys.associateWith { p -> repo.prices(p.id).associate { it.store.id to it.price } }
        val unavailable = items.keys.filter { pricesByProduct.getValue(it).isEmpty() }
        val relevant = items.filterKeys { it !in unavailable }

        val baskets = stores.map { store ->
            var total = 0
            val missing = mutableListOf<Product>()
            for ((product, qty) in relevant) {
                val price = pricesByProduct.getValue(product)[store.id]
                if (price == null) missing += product else total += price * qty
            }
            StoreBasket(store, total, missing)
        }
        val sorted = baskets.sortedWith(
            compareBy<StoreBasket>({ !it.complete }, { it.missing.size }, { it.total }),
        )
        val split = relevant.entries.sumOf { (product, qty) ->
            (pricesByProduct.getValue(product).values.minOrNull() ?: 0) * qty
        }
        return BasketComparison(sorted, split, unavailable)
    }
}
