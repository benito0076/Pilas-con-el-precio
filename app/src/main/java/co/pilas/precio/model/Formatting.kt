package co.pilas.precio.model

/** Formatea pesos colombianos con punto de miles: 12900 -> "$12.900". */
fun formatCop(amount: Int): String {
    val negative = amount < 0
    val digits = kotlin.math.abs(amount).toString()
    val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
    return (if (negative) "-$" else "$") + grouped
}

/** Precio por unidad de comparación (100 g, 100 ml o 1 un), como Double. */
fun unitPrice(price: Int, product: Product): Double =
    price.toDouble() / product.quantity * product.unit.comparisonBase

fun formatUnitPrice(price: Int, product: Product): String {
    val value = unitPrice(price, product)
    val text = if (value >= 100) formatCop(Math.round(value).toInt()) else "$" + "%.1f".format(java.util.Locale.US, value).replace('.', ',')
    return "$text ${product.unit.comparisonLabel}"
}
