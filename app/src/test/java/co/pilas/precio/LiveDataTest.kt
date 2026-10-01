package co.pilas.precio

import co.pilas.precio.data.CategoryGuesser
import co.pilas.precio.data.LiveProduct
import co.pilas.precio.data.OfferMerger
import co.pilas.precio.data.QuantityParser
import co.pilas.precio.data.SeedData
import co.pilas.precio.data.SnapshotCodec
import co.pilas.precio.data.VtexParser
import co.pilas.precio.model.Category
import co.pilas.precio.model.MeasureUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveDataTest {
    private val exito = SeedData.stores.first { it.id == "exito" }
    private val jumbo = SeedData.stores.first { it.id == "jumbo" }

    private fun vtex(name: String, ean: String, price: Double, qty: Int = 10) = """
        {"productId":"1","productName":"$name","brand":"Diana","categories":["/Despensa/Arroz/"],
         "items":[{"itemId":"1","ean":"$ean","sellers":[{"sellerId":"1",
         "commertialOffer":{"Price":$price,"ListPrice":$price,"AvailableQuantity":$qty}}]}]}
    """

    @Test
    fun parsesVtexResponseAndSkipsUnavailable() {
        val json = "[" + vtex("Arroz Diana 1000 g", "7701234567890", 4590.0) + "," +
            vtex("Agotado", "7700000000001", 1000.0, qty = 0) + "]"
        val offers = VtexParser.parse(json, exito)
        assertEquals(1, offers.size)
        assertEquals(4590, offers[0].price)
        assertEquals("7701234567890", offers[0].ean)
    }

    @Test
    fun parsesQuantities() {
        assertEquals(1000 to MeasureUnit.GRAMOS, QuantityParser.parse("Arroz Diana 1 kg"))
        assertEquals(500 to MeasureUnit.GRAMOS, QuantityParser.parse("Lentejas 500 g"))
        assertEquals(1500 to MeasureUnit.MILILITROS, QuantityParser.parse("Gaseosa 1,5 L"))
        assertEquals(1500 to MeasureUnit.MILILITROS, QuantityParser.parse("Gaseosa 6 x 250 ml"))
        assertEquals(12 to MeasureUnit.UNIDADES, QuantityParser.parse("Papel higiénico x12"))
        assertEquals(1 to MeasureUnit.UNIDADES, QuantityParser.parse("Escoba"))
    }

    @Test
    fun mergesSameEanAcrossStores() {
        val a = VtexParser.parse("[" + vtex("Arroz Diana 1000 g", "07701234567890", 4590.0) + "]", exito)
        val b = VtexParser.parse("[" + vtex("Arroz Diana Premium 1000 g", "7701234567890", 4300.0) + "]", jumbo)
        val merged = OfferMerger.merge(a + b)
        assertEquals(1, merged.size)
        assertEquals(listOf(4300, 4590), merged[0].prices.map { it.price })
        assertTrue(merged[0].product.id.startsWith("live-"))
    }

    @Test
    fun snapshotRoundTrips() {
        val a = VtexParser.parse("[" + vtex("Arroz Diana 1000 g", "7701234567890", 4590.0) + "]", exito)
        val item = OfferMerger.merge(a).single()
        val back = SnapshotCodec.decode(item.product.id, SnapshotCodec.encode(item))
        assertNotNull(back)
        assertEquals(item.product, (back as LiveProduct).product)
        assertEquals(item.prices.map { it.price }, back.prices.map { it.price })
    }

    @Test
    fun guessesCategory() {
        assertEquals(Category.LACTEOS, CategoryGuesser.guess("/Lácteos/", "Leche"))
        assertEquals(Category.OTROS, CategoryGuesser.guess("", "Taladro"))
    }
}
