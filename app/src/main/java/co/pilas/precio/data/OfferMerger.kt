package co.pilas.precio.data

import co.pilas.precio.model.Product
import co.pilas.precio.model.StorePrice

data class LiveProduct(val product: Product, val prices: List<StorePrice>)

object OfferMerger {

    /** Agrupa ofertas de distintas tiendas del mismo producto (por EAN; si no hay, por marca y nombre). */
    fun merge(offers: List<Offer>): List<LiveProduct> {
        val groups = offers.groupBy { o ->
            o.ean?.trimStart('0')?.takeIf { it.length >= 8 }?.let { "ean:$it" }
                ?: "n:" + (o.brand + "|" + o.name).normalized()
        }
        return groups.map { (key, group) ->
            val first = group.first()
            val (qty, unit) = QuantityParser.parse(first.name)
            val product = Product(
                id = "live-" + key.hashCode().toUInt().toString(16),
                name = first.name,
                brand = first.brand.ifBlank { "Sin marca" },
                category = CategoryGuesser.guess(first.categoryPath, first.name),
                quantity = qty,
                unit = unit,
            )
            // Una oferta por tienda: la más barata si la tienda repite el producto.
            val prices = group.groupBy { it.store.id }
                .map { (_, list) -> list.minBy { it.price } }
                .map { StorePrice(it.store, it.price) }
                .sortedBy { it.price }
            LiveProduct(product, prices)
        }
    }
}
