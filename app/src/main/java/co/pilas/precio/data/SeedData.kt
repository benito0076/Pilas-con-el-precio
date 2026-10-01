package co.pilas.precio.data

import co.pilas.precio.model.Category
import co.pilas.precio.model.MeasureUnit
import co.pilas.precio.model.Product
import co.pilas.precio.model.Store
import co.pilas.precio.model.StorePrice

/**
 * Catálogo de ejemplo. Los precios son ILUSTRATIVOS (aproximados, en COP) y se
 * generan de forma determinista a partir de un precio de referencia por producto;
 * no corresponden a precios reales de las tiendas. Se reemplazan implementando
 * [PriceRepository] contra una fuente real (API, aportes de usuarios, etc.).
 */
object SeedData {

    val stores: List<Store> = listOf(
        Store("exito", "Éxito", "Hipermercado"),
        Store("jumbo", "Jumbo", "Hipermercado"),
        Store("carulla", "Carulla", "Supermercado"),
        Store("olimpica", "Olímpica", "Supermercado"),
        Store("d1", "D1", "Tienda de descuento"),
        Store("ara", "Ara", "Tienda de descuento"),
        Store("makro", "Makro", "Mayorista"),
    )

    /** Factor de precio y porcentaje de surtido (0-100) de cada tienda. */
    private data class StoreProfile(val factor: Double, val assortment: Int)

    private val profiles = mapOf(
        "exito" to StoreProfile(1.00, 95),
        "jumbo" to StoreProfile(1.03, 95),
        "carulla" to StoreProfile(1.07, 90),
        "olimpica" to StoreProfile(0.98, 90),
        "d1" to StoreProfile(0.93, 60),
        "ara" to StoreProfile(0.94, 60),
        "makro" to StoreProfile(0.90, 55),
    )

    private class Seed(val product: Product, val basePrice: Int)

    private fun p(
        id: String, name: String, brand: String, cat: Category,
        qty: Int, unit: MeasureUnit, price: Int,
    ) = Seed(Product(id, name, brand, cat, qty, unit), price)

    private val G = MeasureUnit.GRAMOS
    private val ML = MeasureUnit.MILILITROS
    private val UN = MeasureUnit.UNIDADES

    private val seeds: List<Seed> = listOf(
        // Despensa
        p("arroz-diana-1k", "Arroz blanco", "Diana", Category.DESPENSA, 1000, G, 4500),
        p("arroz-roa-500", "Arroz blanco", "Roa", Category.DESPENSA, 500, G, 2600),
        p("aceite-gourmet-1l", "Aceite de girasol", "Gourmet", Category.DESPENSA, 1000, ML, 14500),
        p("azucar-incauca-1k", "Azúcar blanca", "Incauca", Category.DESPENSA, 1000, G, 4300),
        p("sal-refisal-500", "Sal refinada", "Refisal", Category.DESPENSA, 500, G, 1800),
        p("frijol-cargamanto-500", "Fríjol cargamanto", "Zenú", Category.DESPENSA, 500, G, 7500),
        p("lentejas-500", "Lentejas", "Diana", Category.DESPENSA, 500, G, 4200),
        p("pasta-doria-250", "Espagueti", "Doria", Category.DESPENSA, 250, G, 3300),
        p("atun-vancamps-160", "Atún en agua", "Van Camp's", Category.DESPENSA, 160, G, 6500),
        p("harina-pan-1k", "Harina de maíz precocida", "PAN", Category.DESPENSA, 1000, G, 4800),
        p("chocolate-corona-500", "Chocolate de mesa", "Corona", Category.DESPENSA, 500, G, 12500),
        p("cafe-sello-rojo-500", "Café molido", "Sello Rojo", Category.DESPENSA, 500, G, 17500),
        p("salsa-tomate-fruco-400", "Salsa de tomate", "Fruco", Category.DESPENSA, 400, G, 6500),
        // Lácteos y huevos
        p("leche-alqueria-1l", "Leche entera", "Alquería", Category.LACTEOS, 1000, ML, 4600),
        p("leche-colanta-1100", "Leche entera", "Colanta", Category.LACTEOS, 1100, ML, 4900),
        p("huevos-aa-30", "Huevos AA", "Santa Reyes", Category.LACTEOS, 30, UN, 17500),
        p("queso-campesino-500", "Queso campesino", "Colanta", Category.LACTEOS, 500, G, 11500),
        p("yogurt-alpina-1l", "Yogurt entero", "Alpina", Category.LACTEOS, 1000, ML, 7500),
        // Carnes
        p("pechuga-pollo-1k", "Pechuga de pollo", "Pollo Fresco", Category.CARNES, 1000, G, 15500),
        p("carne-molida-500", "Carne molida de res", "Res Selecta", Category.CARNES, 500, G, 13500),
        p("salchichas-zenu-500", "Salchichas", "Zenú", Category.CARNES, 500, G, 10500),
        // Frutas y verduras
        p("papa-pastusa-1k", "Papa pastusa", "Granel", Category.FRUVER, 1000, G, 3200),
        p("tomate-1k", "Tomate chonto", "Granel", Category.FRUVER, 1000, G, 4500),
        p("cebolla-cabezona-1k", "Cebolla cabezona", "Granel", Category.FRUVER, 1000, G, 3800),
        p("platano-verde-1k", "Plátano verde", "Granel", Category.FRUVER, 1000, G, 3000),
        p("banano-1k", "Banano", "Granel", Category.FRUVER, 1000, G, 3500),
        p("zanahoria-1k", "Zanahoria", "Granel", Category.FRUVER, 1000, G, 2800),
        p("aguacate-hass-1k", "Aguacate Hass", "Granel", Category.FRUVER, 1000, G, 12000),
        // Bebidas
        p("coca-cola-1500", "Gaseosa Coca-Cola", "Coca-Cola", Category.BEBIDAS, 1500, ML, 5500),
        p("agua-cristal-600", "Agua sin gas", "Cristal", Category.BEBIDAS, 600, ML, 2200),
        p("jugo-hit-1l", "Jugo de mango", "Hit", Category.BEBIDAS, 1000, ML, 4800),
        // Panadería y snacks
        p("pan-bimbo-450", "Pan tajado", "Bimbo", Category.PANADERIA, 450, G, 7800),
        p("galletas-saltin-300", "Galletas saladas", "Saltín Noel", Category.PANADERIA, 300, G, 4200),
        p("papas-margarita-150", "Papas fritas pollo", "Margarita", Category.PANADERIA, 150, G, 5000),
        // Aseo del hogar
        p("detergente-fab-1800", "Detergente en polvo", "Fab", Category.ASEO_HOGAR, 1800, G, 15500),
        p("blanqueador-limpido-1l", "Blanqueador", "Límpido", Category.ASEO_HOGAR, 1000, ML, 3800),
        p("papel-familia-12", "Papel higiénico", "Familia", Category.ASEO_HOGAR, 12, UN, 18500),
        p("jabon-rey-300", "Jabón de ropa en barra", "Rey", Category.ASEO_HOGAR, 300, G, 3600),
        p("lavaloza-axion-450", "Lavaloza en crema", "Axion", Category.ASEO_HOGAR, 450, G, 5800),
        p("suavizante-suavitel-1l", "Suavizante de ropa", "Suavitel", Category.ASEO_HOGAR, 1000, ML, 9500),
        // Cuidado personal
        p("crema-dental-colgate-100", "Crema dental", "Colgate", Category.ASEO_PERSONAL, 100, G, 6500),
        p("jabon-palmolive-110", "Jabón de baño", "Palmolive", Category.ASEO_PERSONAL, 110, G, 3200),
        p("shampoo-hys-375", "Shampoo", "Head & Shoulders", Category.ASEO_PERSONAL, 375, ML, 18000),
        p("desodorante-rexona-150", "Desodorante en aerosol", "Rexona", Category.ASEO_PERSONAL, 150, ML, 12500),
        p("toallas-nosotras-10", "Toallas higiénicas", "Nosotras", Category.ASEO_PERSONAL, 10, UN, 6500),
    )

    val products: List<Product> = seeds.map { it.product }

    /** Hash estable (independiente de la plataforma) para variar precios sin azar. */
    private fun stableHash(text: String): Int {
        var h = 17
        for (c in text) h = h * 31 + c.code
        return kotlin.math.abs(h % 10_000)
    }

    private fun roundTo50(value: Double): Int = (Math.round(value / 50.0) * 50).toInt()

    /** Precios por producto, ordenados del más barato al más caro. */
    val pricesByProduct: Map<String, List<StorePrice>> = seeds.associate { seed ->
        val list = stores.mapNotNull { store ->
            val profile = profiles.getValue(store.id)
            val h = stableHash(seed.product.id + "|" + store.id)
            if (h % 100 >= profile.assortment) return@mapNotNull null
            val noise = 1.0 + ((h / 100) % 9 - 4) / 100.0 // -4 % .. +4 %
            StorePrice(store, roundTo50(seed.basePrice * profile.factor * noise).coerceAtLeast(50))
        }.sortedBy { it.price }
        seed.product.id to list
    }
}
