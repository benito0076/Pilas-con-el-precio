package co.pilas.precio.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import co.pilas.precio.data.BasketCalculator
import co.pilas.precio.data.BasketComparison
import co.pilas.precio.data.PriceRepository
import co.pilas.precio.data.SeedData
import co.pilas.precio.data.SeedPriceRepository
import co.pilas.precio.data.ShoppingListStore
import co.pilas.precio.model.Category
import co.pilas.precio.model.Product

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val repo: PriceRepository = SeedPriceRepository()
    private val store = ShoppingListStore(app)

    var query by mutableStateOf("")
    var category by mutableStateOf<Category?>(null)

    /** id de producto -> cantidad. */
    val list = mutableStateMapOf<String, Int>().apply { putAll(store.load()) }

    fun results(): List<Product> = repo.search(query, category)

    fun inList(product: Product): Boolean = list.containsKey(product.id)

    fun add(product: Product) = setQuantity(product, (list[product.id] ?: 0) + 1)

    fun setQuantity(product: Product, quantity: Int) {
        if (quantity <= 0) list.remove(product.id) else list[product.id] = quantity.coerceAtMost(99)
        store.save(list.toMap())
    }

    fun clearList() {
        list.clear()
        store.save(emptyMap())
    }

    fun comparison(): BasketComparison {
        val items = list.mapNotNull { (id, qty) -> repo.product(id)?.let { it to qty } }.toMap()
        return BasketCalculator.compare(items, repo, SeedData.stores)
    }
}
