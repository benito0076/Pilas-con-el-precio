package co.pilas.precio.data

import co.pilas.precio.model.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Tiendas sin API pública: se descarga la página de resultados de búsqueda y se
 * extraen los productos de los datos estructurados incrustados en el HTML.
 * [searchUrl] lleva el marcador {q} donde va la búsqueda codificada.
 */
class HtmlSource(override val store: Store, private val searchUrl: String) : PriceSource {

    override suspend fun search(query: String, limit: Int): List<Offer> = withContext(Dispatchers.IO) {
        val url = URL(searchUrl.replace("{q}", URLEncoder.encode(query, "UTF-8")))
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8_000
            conn.readTimeout = 12_000
            conn.setRequestProperty("Accept", "text/html")
            conn.setRequestProperty("Accept-Language", "es-CO,es;q=0.9")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) PilasConElPrecio/0.3")
            val code = conn.responseCode
            check(code in 200..299) { "HTTP $code" }
            val offers = HtmlExtractor.extract(conn.inputStream.bufferedReader().use { it.readText() }, store)
            check(offers.isNotEmpty()) { "sin productos reconocibles en la página" }
            // Páginas fijas (sin {q}), como un catálogo de ofertas: se filtra aquí por la búsqueda.
            val result = if ("{q}" in searchUrl) offers else HtmlExtractor.filterByQuery(offers, query)
            result.take(limit)
        } finally {
            conn.disconnect()
        }
    }
}

object HtmlExtractor {
    private val scriptRegex = Regex(
        """<script[^>]*type\s*=\s*["'](application/ld\+json|application/json)["'][^>]*>(.*?)</script>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )

    fun extract(html: String, store: Store): List<Offer> {
        val offers = mutableListOf<Offer>()
        for (m in scriptRegex.findAll(html)) {
            val isLd = m.groupValues[1].contains("ld+json", ignoreCase = true)
            val root = parseJson(m.groupValues[2].trim()) ?: continue
            if (isLd) collectLd(root, store, offers) else collectGeneric(root, store, offers)
        }
        // Una página puede repetir el mismo producto en varios bloques.
        return offers.distinctBy { it.name.lowercase() + "|" + it.price }
    }

    /** Deja las ofertas cuyo nombre o marca contiene todas las palabras de la búsqueda (sin tildes). */
    fun filterByQuery(offers: List<Offer>, query: String): List<Offer> {
        val words = query.normalized().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return offers.filter { o ->
            val text = "${o.name} ${o.brand}".normalized()
            words.all { it in text }
        }
    }

    private fun parseJson(text: String): Any? = runCatching {
        if (text.startsWith("[")) JSONArray(text) else JSONObject(text)
    }.getOrNull()

    /** schema.org: objetos de tipo Product con "offers" (también dentro de @graph e ItemList). */
    private fun collectLd(node: Any?, store: Store, out: MutableList<Offer>) {
        when (node) {
            is JSONArray -> for (i in 0 until node.length()) collectLd(node.opt(i), store, out)
            is JSONObject -> {
                if (typeOf(node).any { it.equals("Product", ignoreCase = true) }) {
                    toLdOffer(node, store)?.let { out += it }
                }
                for (key in node.keys()) collectLd(node.opt(key), store, out)
            }
        }
    }

    private fun typeOf(o: JSONObject): List<String> = when (val t = o.opt("@type")) {
        is String -> listOf(t)
        is JSONArray -> (0 until t.length()).map { t.optString(it) }
        else -> emptyList()
    }

    private fun toLdOffer(p: JSONObject, store: Store): Offer? {
        val name = p.optString("name").takeIf { it.isNotBlank() } ?: return null
        val offers = p.opt("offers")
        val offer = when (offers) {
            is JSONObject -> offers
            is JSONArray -> (0 until offers.length()).mapNotNull { offers.optJSONObject(it) }
                .firstOrNull { !it.optString("availability").contains("OutOfStock", true) }
            else -> null
        } ?: return null
        if (offer.optString("availability").contains("OutOfStock", ignoreCase = true)) return null
        val price = parsePrice(offer.opt("price")) ?: parsePrice(offer.opt("lowPrice")) ?: return null
        val brand = when (val b = p.opt("brand")) {
            is JSONObject -> b.optString("name")
            is String -> b
            else -> ""
        }
        val ean = listOf("gtin13", "gtin", "gtin14", "gtin12").map { p.optString(it) }.firstOrNull { it.isNotBlank() }
        return Offer(store, name, brand, ean, price, p.optString("category"))
    }

    /** Respaldo para páginas Next.js u otras con JSON incrustado: objetos con "name" y "price" numérico. */
    private fun collectGeneric(node: Any?, store: Store, out: MutableList<Offer>) {
        when (node) {
            is JSONArray -> for (i in 0 until node.length()) collectGeneric(node.opt(i), store, out)
            is JSONObject -> {
                val name = node.opt("name") as? String ?: node.opt("productName") as? String
                val price = parsePrice(node.opt("price")) ?: parsePrice(node.opt("sellingPrice"))
                if (!name.isNullOrBlank() && price != null) {
                    val brand = when (val b = node.opt("brand")) {
                        is JSONObject -> b.optString("name")
                        is String -> b
                        else -> ""
                    }
                    val ean = listOf("ean", "gtin", "barcode").map { node.optString(it) }.firstOrNull { it.isNotBlank() }
                    out += Offer(store, name, brand, ean, price, node.optString("category"))
                }
                for (key in node.keys()) collectGeneric(node.opt(key), store, out)
            }
        }
    }

    /** Acepta 4590, 4590.0, "4590", "4.590" y "$ 4.590,00"; descarta precios no positivos. */
    fun parsePrice(value: Any?): Int? {
        val d = when (value) {
            is Number -> value.toDouble()
            is String -> parseText(value) ?: return null
            else -> return null
        }
        return if (d >= 1) Math.round(d).toInt() else null
    }

    private fun parseText(raw: String): Double? {
        val s = raw.filter { it.isDigit() || it == '.' || it == ',' }
        if (s.isEmpty()) return null
        val normalized = when {
            Regex("""\d{1,3}(\.\d{3})+(,\d+)?""").matches(s) -> s.replace(".", "").replace(',', '.') // 4.590,00
            Regex("""\d{1,3}(,\d{3})+(\.\d+)?""").matches(s) -> s.replace(",", "")                   // 4,590.00
            else -> s.replace(',', '.')
        }
        return normalized.toDoubleOrNull()
    }
}
