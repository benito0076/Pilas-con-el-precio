package co.pilas.precio.data

import co.pilas.precio.model.Category
import co.pilas.precio.model.Product
import co.pilas.precio.model.StorePrice
import java.text.Normalizer

interface PriceRepository {
    fun products(): List<Product>
    fun product(id: String): Product?

    /** Precios disponibles del producto, del más barato al más caro. */
    fun prices(productId: String): List<StorePrice>

    fun search(query: String, category: Category?): List<Product> {
        val q = query.normalized()
        return products().filter { p ->
            (category == null || p.category == category) &&
                (q.isEmpty() || "${p.name} ${p.brand}".normalized().contains(q))
        }
    }
}

/** Repositorio que sirve los datos de ejemplo incluidos en la app. */
class SeedPriceRepository : PriceRepository {
    private val byId = SeedData.products.associateBy { it.id }
    override fun products(): List<Product> = SeedData.products
    override fun product(id: String): Product? = byId[id]
    override fun prices(productId: String): List<StorePrice> =
        SeedData.pricesByProduct[productId].orEmpty()
}

/** Minúsculas y sin tildes, para que "cafe" encuentre "Café". */
fun String.normalized(): String =
    Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
