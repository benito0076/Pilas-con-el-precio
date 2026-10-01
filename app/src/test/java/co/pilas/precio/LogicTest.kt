package co.pilas.precio

import co.pilas.precio.data.BasketCalculator
import co.pilas.precio.data.SeedData
import co.pilas.precio.data.SeedPriceRepository
import co.pilas.precio.model.Category
import co.pilas.precio.model.formatCop
import co.pilas.precio.model.formatUnitPrice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {
    private val repo = SeedPriceRepository()

    @Test
    fun formatsPesos() {
        assertEquals("$0", formatCop(0))
        assertEquals("$950", formatCop(950))
        assertEquals("$12.900", formatCop(12900))
        assertEquals("$1.234.500", formatCop(1234500))
    }

    @Test
    fun unitPriceIsNormalized() {
        val arroz = repo.product("arroz-diana-1k")!!
        assertEquals("$450 por 100 g", formatUnitPrice(4500, arroz))
        val huevos = repo.product("huevos-aa-30")!!
        assertEquals("$600 por unidad", formatUnitPrice(18000, huevos))
    }

    @Test
    fun searchIgnoresAccentsAndCase() {
        assertTrue(repo.search("CAFE", null).any { it.id == "cafe-sello-rojo-500" })
        assertTrue(repo.search("", Category.LACTEOS).all { it.category == Category.LACTEOS })
        assertTrue(repo.search("zzzz", null).isEmpty())
    }

    @Test
    fun seedPricesAreSortedAndPositive() {
        SeedData.products.forEach { p ->
            val prices = repo.prices(p.id)
            assertEquals(prices.sortedBy { it.price }, prices)
            assertTrue(prices.all { it.price > 0 })
        }
        assertTrue(SeedData.products.map { it.id }.toSet().size == SeedData.products.size)
    }

    @Test
    fun basketComparisonPicksCheapestCompleteStore() {
        val items = listOf("arroz-diana-1k", "leche-alqueria-1l", "azucar-incauca-1k")
            .associate { repo.product(it)!! to 2 }
        val c = BasketCalculator.compare(items, repo, SeedData.stores)
        val best = c.bestSingleStore
        if (best != null) {
            assertTrue(c.byStore.filter { it.complete }.all { it.total >= best.total })
            assertTrue(c.splitTotal <= best.total)
        }
        val expectedSplit = items.entries.sumOf { (p, q) -> repo.prices(p.id).minOf { it.price } * q }
        assertEquals(expectedSplit, c.splitTotal)
    }
}
