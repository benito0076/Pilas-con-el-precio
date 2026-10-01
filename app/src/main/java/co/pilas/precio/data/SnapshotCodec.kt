package co.pilas.precio.data

import co.pilas.precio.model.Category
import co.pilas.precio.model.MeasureUnit
import co.pilas.precio.model.Product
import org.json.JSONArray
import org.json.JSONObject

/** Serializa un producto en vivo con sus precios, para conservarlo en la lista sin conexión. */
object SnapshotCodec {

    fun encode(item: LiveProduct): String = JSONObject().apply {
        put("name", item.product.name)
        put("brand", item.product.brand)
        put("category", item.product.category.name)
        put("quantity", item.product.quantity)
        put("unit", item.product.unit.name)
        put("prices", JSONArray().apply {
            item.prices.forEach { put(JSONObject().put("store", it.store.id).put("price", it.price)) }
        })
    }.toString()

    fun decode(id: String, json: String): LiveProduct? = runCatching {
        val o = JSONObject(json)
        val product = Product(
            id = id,
            name = o.getString("name"),
            brand = o.getString("brand"),
            category = Category.valueOf(o.getString("category")),
            quantity = o.getInt("quantity"),
            unit = MeasureUnit.valueOf(o.getString("unit")),
        )
        val arr = o.getJSONArray("prices")
        val prices = (0 until arr.length()).mapNotNull { i ->
            val p = arr.getJSONObject(i)
            SeedData.stores.firstOrNull { it.id == p.getString("store") }
                ?.let { co.pilas.precio.model.StorePrice(it, p.getInt("price")) }
        }.sortedBy { it.price }
        LiveProduct(product, prices)
    }.getOrNull()
}
