package co.pilas.precio

import co.pilas.precio.data.HtmlExtractor
import co.pilas.precio.data.SeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HtmlExtractorTest {
    private val d1 = SeedData.stores.first { it.id == "d1" }

    @Test
    fun extractsJsonLdProductsFromGraphAndLists() {
        val html = """
            <html><head>
            <script type="application/ld+json">{"@context":"https://schema.org","@graph":[
              {"@type":"Product","name":"Arroz Blanco 500 g","brand":{"@type":"Brand","name":"Roa"},
               "gtin13":"7701111111111","offers":{"@type":"Offer","price":"2.490","availability":"https://schema.org/InStock"}},
              {"@type":"Product","name":"Agotado 1 kg","offers":{"price":1000,"availability":"https://schema.org/OutOfStock"}}
            ]}</script>
            <script type="application/ld+json">[{"@type":["Product"],"name":"Sal 500 g","brand":"Refisal",
              "offers":[{"price":1800.00}]}]</script>
            </head></html>"""
        val offers = HtmlExtractor.extract(html, d1)
        assertEquals(listOf("Arroz Blanco 500 g", "Sal 500 g"), offers.map { it.name })
        assertEquals(listOf(2490, 1800), offers.map { it.price })
        assertEquals("Roa", offers[0].brand)
        assertEquals("7701111111111", offers[0].ean)
    }

    @Test
    fun fallsBackToEmbeddedJson() {
        val html = """<script id="__NEXT_DATA__" type="application/json">
            {"props":{"pageProps":{"products":[{"name":"Leche 1100 ml","price":4900,"brand":"Colanta","ean":"7702222222222"}]}}}
            </script>"""
        val offers = HtmlExtractor.extract(html, d1)
        assertEquals(1, offers.size)
        assertEquals(4900, offers[0].price)
        assertEquals("Colanta", offers[0].brand)
    }

    @Test
    fun parsesColombianPriceFormats() {
        assertEquals(4590, HtmlExtractor.parsePrice("$ 4.590"))
        assertEquals(4590, HtmlExtractor.parsePrice("4.590,00"))
        assertEquals(4590, HtmlExtractor.parsePrice("4,590.00"))
        assertEquals(4590, HtmlExtractor.parsePrice(4590.4))
        assertNull(HtmlExtractor.parsePrice("gratis"))
        assertNull(HtmlExtractor.parsePrice(0))
    }

    @Test
    fun pageWithoutStructuredDataGivesNothing() {
        assertEquals(0, HtmlExtractor.extract("<html><body>hola</body></html>", d1).size)
    }
}
