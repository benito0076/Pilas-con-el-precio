package co.pilas.precio.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import co.pilas.precio.data.BasketCalculator
import co.pilas.precio.data.BasketComparison
import co.pilas.precio.data.CompositeRepository
import co.pilas.precio.data.LiveProduct
import co.pilas.precio.data.LiveResult
import co.pilas.precio.data.LiveSearch
import co.pilas.precio.data.PriceRepository
import co.pilas.precio.data.SeedData
import co.pilas.precio.data.SeedPriceRepository
import co.pilas.precio.data.ShoppingListStore
import co.pilas.precio.model.Category
import co.pilas.precio.model.Product
import co.pilas.precio.model.isLive
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MIN_LIVE_QUERY = 3

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ShoppingListStore(app)
    private val liveSearch = LiveSearch.default()
    private val demo = SeedPriceRepository()

    /** Productos en vivo conocidos (búsquedas recientes + los guardados en la lista). */
    private val liveCache = mutableMapOf<String, LiveProduct>().apply {
        store.loadSnapshots().forEach { put(it.product.id, it) }
    }
    val repo: PriceRepository = CompositeRepository(demo, liveCache)

    var query by mutableStateOf("")
        private set
    var category by mutableStateOf<Category?>(null)

    /** Estado de la última búsqueda en vivo (null = aún no se ha buscado). */
    var liveResult by mutableStateOf<LiveResult?>(null)
        private set
    var searching by mutableStateOf(false)
        private set
    private var liveIds by mutableStateOf<List<String>>(emptyList())
    private var searchJob: Job? = null

    /** id de producto -> cantidad. */
    val list = mutableStateMapOf<String, Int>().apply { putAll(store.load()) }

    /** True si la lista de resultados viene de las tiendas (y no de la demostración). */
    val showingLive: Boolean
        get() = query.trim().length >= MIN_LIVE_QUERY && liveResult?.let { it.answered.isNotEmpty() } == true

    fun onQueryChange(text: String) {
        query = text
        searchJob?.cancel()
        liveResult = null
        liveIds = emptyList()
        if (text.trim().length < MIN_LIVE_QUERY) {
            searching = false
            return
        }
        searching = true
        searchJob = viewModelScope.launch {
            delay(500) // espera a que el usuario termine de escribir
            val result = liveSearch.search(text.trim())
            result.products.forEach { liveCache[it.product.id] = it }
            liveIds = result.products.map { it.product.id }
            liveResult = result
            searching = false
        }
    }

    fun results(): List<Product> {
        if (showingLive) {
            return liveIds.mapNotNull { liveCache[it]?.product }
                .filter { category == null || it.category == category }
        }
        return demo.search(query, category)
    }

    fun inList(product: Product): Boolean = list.containsKey(product.id)

    fun add(product: Product) = setQuantity(product, (list[product.id] ?: 0) + 1)

    fun setQuantity(product: Product, quantity: Int) {
        if (quantity <= 0) {
            list.remove(product.id)
            if (product.isLive) store.removeSnapshot(product.id)
        } else {
            list[product.id] = quantity.coerceAtMost(99)
            if (product.isLive) liveCache[product.id]?.let { store.saveSnapshot(it) }
        }
        store.save(list.toMap())
    }

    fun clearList() {
        list.keys.filter { it.startsWith("live-") }.forEach { store.removeSnapshot(it) }
        list.clear()
        store.save(emptyMap())
    }

    fun comparison(): BasketComparison {
        val items = list.mapNotNull { (id, qty) -> repo.product(id)?.let { it to qty } }.toMap()
        return BasketCalculator.compare(items, repo, SeedData.stores)
    }
}
