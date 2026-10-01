package co.pilas.precio.model

/** Categorías de la canasta familiar. */
enum class Category(val label: String, val emoji: String) {
    DESPENSA("Despensa", "🍚"),
    LACTEOS("Lácteos y huevos", "🥛"),
    CARNES("Carnes", "🍗"),
    FRUVER("Frutas y verduras", "🥑"),
    BEBIDAS("Bebidas", "🥤"),
    PANADERIA("Panadería y snacks", "🍞"),
    ASEO_HOGAR("Aseo del hogar", "🧴"),
    ASEO_PERSONAL("Cuidado personal", "🪥"),
    OTROS("Otros", "🛒"),
}

/** Unidad base en la que se mide el contenido de un producto. */
enum class MeasureUnit(val shortLabel: String, val comparisonBase: Int) {
    GRAMOS("g", 100),
    MILILITROS("ml", 100),
    UNIDADES("un", 1);

    /** Texto para el precio normalizado, p. ej. "por 100 g". */
    val comparisonLabel: String
        get() = if (comparisonBase == 1) "por unidad" else "por $comparisonBase $shortLabel"
}

data class Store(val id: String, val name: String, val kind: String)

data class Product(
    val id: String,
    val name: String,
    val brand: String,
    val category: Category,
    /** Contenido total expresado en [unit] (p. ej. 1000 g, 30 unidades). */
    val quantity: Int,
    val unit: MeasureUnit,
) {
    val presentation: String
        get() = when {
            unit == MeasureUnit.UNIDADES -> "$quantity un"
            quantity >= 1000 && quantity % 1000 == 0 ->
                "${quantity / 1000} ${if (unit == MeasureUnit.GRAMOS) "kg" else "L"}"
            else -> "$quantity ${unit.shortLabel}"
        }
}

/** Los productos de precios en vivo tienen id con este prefijo. */
val Product.isLive: Boolean get() = id.startsWith("live-")

/** Precio en pesos colombianos (COP, sin decimales) de un producto en una tienda. */
data class StorePrice(val store: Store, val price: Int)
