package co.pilas.precio.data

import android.content.Context

/** Persiste la lista de compras (id de producto -> cantidad) en SharedPreferences. */
class ShoppingListStore(context: Context) {
    private val prefs = context.getSharedPreferences("shopping_list", Context.MODE_PRIVATE)

    fun load(): Map<String, Int> =
        prefs.all.mapNotNull { (id, qty) -> (qty as? Int)?.takeIf { it > 0 }?.let { id to it } }.toMap()

    fun save(items: Map<String, Int>) {
        prefs.edit().clear().apply { items.forEach { (id, qty) -> putInt(id, qty) } }.apply()
    }
}
