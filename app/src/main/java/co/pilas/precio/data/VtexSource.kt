package co.pilas.precio.data

import co.pilas.precio.model.Category
import co.pilas.precio.model.MeasureUnit
import co.pilas.precio.model.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Oferta de un producto en una tienda, tal como la publica la tienda. */
data class Offer(
    val store: Store,
    val name: String,
    val brand: String,
    val ean: String?,
    val price: Int,
    val categoryPath: String,
)

interface PriceSource {
    val store: Store
    suspend fun search(query: String, limit: Int = 12): List<Offer>
}

/**
 * Tiendas construidas sobre VTEX (Éxito, Carulla, Jumbo, Olímpica) exponen el
 * catálogo público que usa su propia web: /api/catalog_system/pub/products/search.
 */
class VtexSource(override val store: Store, private val host: String) : PriceSource {

    override suspend fun search(query: String, limit: Int): List<Offer> = withContext(Dispatchers.IO) {
        val url = URL("https://$host/api/catalog_system/pub/products/search" +
            "?ft=${URLEncoder.encode(query, "UTF-8")}&_from=0&_to=${limit - 1}")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "PilasConElPrecio/0.2 (Android)")
            val code = conn.responseCode
            check(code in 200..299) { "HTTP $code" }
            VtexParser.parse(conn.inputStream.bufferedReader().use { it.readText() }, store)
        } finally {
            conn.disconnect()
        }
    }
}

object VtexParser {
    /** Interpreta la respuesta JSON de VTEX; ignora productos sin stock o sin precio. */
    fun parse(json: String, store: Store): List<Offer> {
        val array = JSONArray(json)
        val offers = mutableListOf<Offer>()
        for (i in 0 until array.length()) {
            val product = array.optJSONObject(i) ?: continue
            val items = product.optJSONArray("items") ?: continue
            val item = items.optJSONObject(0) ?: continue
            val sellers = item.optJSONArray("sellers") ?: continue
            var price = 0.0
            for (j in 0 until sellers.length()) {
                // "commertialOffer" (sic) es el nombre real del campo en la API de VTEX.
                val offer = sellers.optJSONObject(j)?.optJSONObject("commertialOffer") ?: continue
                if (offer.optInt("AvailableQuantity", 0) > 0 && offer.optDouble("Price", 0.0) > 0) {
                    price = offer.optDouble("Price")
                    break
                }
            }
            if (price <= 0) continue
            val name = product.optString("productName")
            if (name.isBlank()) continue
            offers += Offer(
                store = store,
                name = name,
                brand = product.optString("brand"),
                ean = item.optString("ean").takeIf { it.isNotBlank() },
                price = Math.round(price).toInt(),
                categoryPath = product.optJSONArray("categories")?.optString(0).orEmpty(),
            )
        }
        return offers
    }
}

/** Extrae el contenido (g, ml o unidades) del nombre: "Arroz 1 kg", "6 x 250 ml", "x12". */
object QuantityParser {
    private val multi = Regex("""(\d+)\s*x\s*(\d+(?:[.,]\d+)?)\s*(kg|kilos?|gr?s?|gramos?|lb|libras?|lts?|l|litros?|ml|cc)\b""")
    private val single = Regex("""(\d+(?:[.,]\d+)?)\s*(kg|kilos?|gr?s?|gramos?|lb|libras?|lts?|l|litros?|ml|cc|unds?|un|unidades?)\b""")
    private val pack = Regex("""\bx\s*(\d+)\b""")

    fun parse(name: String): Pair<Int, MeasureUnit> {
        val text = name.lowercase()
        multi.find(text)?.let { m ->
            val (qty, unit) = convert(m.groupValues[2], m.groupValues[3])
            return (qty * m.groupValues[1].toInt()) to unit
        }
        single.find(text)?.let { m ->
            val (qty, unit) = convert(m.groupValues[1], m.groupValues[2])
            if (qty > 0) return qty to unit
        }
        pack.find(text)?.let { return it.groupValues[1].toInt().coerceAtLeast(1) to MeasureUnit.UNIDADES }
        return 1 to MeasureUnit.UNIDADES
    }

    private fun convert(number: String, unit: String): Pair<Int, MeasureUnit> {
        val n = number.replace(',', '.').toDouble()
        return when (unit) {
            "kg", "kilo", "kilos" -> Math.round(n * 1000).toInt() to MeasureUnit.GRAMOS
            "lb", "libra", "libras" -> Math.round(n * 500).toInt() to MeasureUnit.GRAMOS
            "l", "lt", "lts", "litro", "litros" -> Math.round(n * 1000).toInt() to MeasureUnit.MILILITROS
            "ml", "cc" -> Math.round(n).toInt() to MeasureUnit.MILILITROS
            "un", "und", "unds", "unidad", "unidades" -> Math.round(n).toInt() to MeasureUnit.UNIDADES
            else -> Math.round(n).toInt() to MeasureUnit.GRAMOS // g, gr, grs, gramo(s)
        }
    }
}

object CategoryGuesser {
    private val rules = listOf(
        Category.LACTEOS to listOf("lacteo", "leche", "huevo", "queso", "yogur"),
        Category.CARNES to listOf("carne", "pollo", "cerdo", "pescado", "embutido", "charcuter"),
        Category.FRUVER to listOf("fruta", "verdura", "hortaliza", "fruver"),
        Category.BEBIDAS to listOf("bebida", "gaseosa", "jugo", "agua", "licor", "cerveza"),
        Category.PANADERIA to listOf("panader", "pan ", "galleta", "snack", "pasabocas", "dulce"),
        Category.ASEO_HOGAR to listOf("aseo", "limpieza", "hogar", "detergente", "ropa", "papel"),
        Category.ASEO_PERSONAL to listOf("personal", "cuidado", "higiene", "belleza", "salud"),
        Category.DESPENSA to listOf("despensa", "granos", "abarrote", "arroz", "aceite", "pasta", "enlatado", "mercado"),
    )

    fun guess(categoryPath: String, name: String): Category {
        val text = "$categoryPath $name".lowercase()
        return rules.firstOrNull { (_, words) -> words.any { it in text } }?.first ?: Category.OTROS
    }
}
