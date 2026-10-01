package co.pilas.precio.data

import android.content.Context

/** Persiste la lista de compras (id -> cantidad) y los datos de los productos en vivo de la lista. */
class ShoppingListStore(context: Context) {
    private val prefs = context.getSharedPreferences("shopping_list", Context.MODE_PRIVATE)
    private val snapshots = context.getSharedPreferences("live_snapshots", Context.MODE_PRIVATE)

    fun load(): Map<String, Int> =
        prefs.all.mapNotNull { (id, qty) -> (qty as? Int)?.takeIf { it > 0 }?.let { id to it } }.toMap()

    fun save(items: Map<String, Int>) {
        prefs.edit().clear().apply { items.forEach { (id, qty) -> putInt(id, qty) } }.apply()
    }

    fun loadSnapshots(): List<LiveProduct> =
        snapshots.all.mapNotNull { (id, json) -> (json as? String)?.let { SnapshotCodec.decode(id, it) } }

    fun saveSnapshot(item: LiveProduct) {
        snapshots.edit().putString(item.product.id, SnapshotCodec.encode(item)).apply()
    }

    fun removeSnapshot(id: String) {
        snapshots.edit().remove(id).apply()
    }
}
