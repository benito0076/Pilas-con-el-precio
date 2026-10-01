package co.pilas.precio.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.pilas.precio.data.BasketComparison
import co.pilas.precio.data.StoreBasket
import co.pilas.precio.model.Category
import co.pilas.precio.model.Product
import co.pilas.precio.model.formatCop
import co.pilas.precio.model.formatUnitPrice
import co.pilas.precio.model.isLive

private val ScreenPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

@Composable
private fun SampleDataBanner() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "Precios de ejemplo (ilustrativos). Aún no provienen de las tiendas reales.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(10.dp),
        )
    }
}

@Composable
private fun LiveStatus(vm: AppViewModel) {
    val r = vm.liveResult ?: return
    val text = buildString {
        append("Precios en vivo de: ${r.answered.joinToString()}.")
        if (r.failed.isNotEmpty()) append(" No respondieron: ${r.failed.joinToString()}.")
    }
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
}

// ───────────────────────── Buscar ─────────────────────────

@Composable
fun SearchScreen(vm: AppViewModel, onOpen: (Product) -> Unit) {
    val results = vm.results()
    Column(Modifier.fillMaxSize().padding(top = 8.dp)) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Pilas con el precio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (vm.showingLive) LiveStatus(vm) else SampleDataBanner()
            OutlinedTextField(
                value = vm.query,
                onValueChange = { vm.onQueryChange(it) },
                label = { Text("Buscar producto o marca") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = vm.category == null,
                    onClick = { vm.category = null },
                    label = { Text("Todas") },
                )
            }
            items(Category.entries) { c ->
                FilterChip(
                    selected = vm.category == c,
                    onClick = { vm.category = if (vm.category == c) null else c },
                    label = { Text("${c.emoji} ${c.label}") },
                )
            }
        }
        val failedAll = vm.liveResult?.let { it.answered.isEmpty() } == true
        if (failedAll) {
            Text(
                "No pudimos consultar las tiendas (sin conexión o sin respuesta). Mostrando precios de ejemplo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        if (vm.searching) {
            Text("Consultando tiendas…", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        if (results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (vm.searching) "" else "No encontramos productos con esa búsqueda.")
            }
        } else {
            LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results, key = { it.id }) { product -> ProductRow(vm, product, onOpen) }
            }
        }
    }
}

@Composable
private fun ProductRow(vm: AppViewModel, product: Product, onOpen: (Product) -> Unit) {
    val best = vm.repo.prices(product.id).firstOrNull()
    Card(Modifier.fillMaxWidth().clickable { onOpen(product) }) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(product.category.emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "${product.brand} · ${product.presentation}",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (best != null) {
                    Text(
                        formatUnitPrice(best.price, product),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (best != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatCop(best.price),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text("en ${best.store.name}", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text("Sin precio", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ───────────────────────── Detalle ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen(vm: AppViewModel, product: Product, onBack: () -> Unit) {
    val prices = vm.repo.prices(product.id)
    val cheapest = prices.firstOrNull()?.price
    val qty = vm.list[product.id] ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(padding),
        ) {
            item {
                Text("${product.brand} · ${product.presentation}", style = MaterialTheme.typography.titleMedium)
                Text(product.category.label, style = MaterialTheme.typography.bodySmall)
            }
            if (!product.isLive) item { SampleDataBanner() }
            item {
                if (qty == 0) {
                    Button(onClick = { vm.add(product) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Agregar a mi lista")
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("En tu lista:", Modifier.weight(1f))
                        QuantityStepper(qty) { vm.setQuantity(product, it) }
                    }
                }
            }
            item {
                Text("Precio por tienda", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (prices.isEmpty()) {
                item { Text("Este producto no tiene precios registrados.") }
            }
            items(prices, key = { it.store.id }) { sp ->
                val isBest = sp.price == cheapest
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isBest) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(sp.store.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                formatUnitPrice(sp.price, product),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatCop(sp.price), fontWeight = FontWeight.Bold)
                            if (isBest) {
                                Text("Más barato", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            } else if (cheapest != null) {
                                Text("+${formatCop(sp.price - cheapest)}", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantityStepper(quantity: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(quantity - 1) }) {
            Icon(
                if (quantity <= 1) Icons.Default.Delete else Icons.Default.Remove,
                contentDescription = if (quantity <= 1) "Quitar" else "Restar uno",
            )
        }
        Text("$quantity", fontWeight = FontWeight.Bold)
        IconButton(onClick = { onChange(quantity + 1) }) {
            Icon(Icons.Default.Add, contentDescription = "Sumar uno")
        }
    }
}

// ───────────────────────── Mi lista ─────────────────────────

@Composable
fun ListScreen(vm: AppViewModel, onOpen: (Product) -> Unit) {
    if (vm.list.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "Tu lista está vacía. Busca productos y agrégalos para saber en qué tienda te sale más barato el mercado.",
            )
        }
        return
    }
    val comparison = vm.comparison()
    val items = vm.list.keys.mapNotNull { vm.repo.product(it) }.sortedBy { it.name }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Mi lista",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { vm.clearList() }) { Text("Vaciar") }
            }
        }
        if (items.any { !it.isLive }) item { SampleDataBanner() }
        item { Summary(comparison) }
        items(comparison.byStore, key = { it.store.id }) { StoreBasketCard(it) }
        item {
            Text("Productos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(items, key = { it.id }) { product ->
            Card(Modifier.fillMaxWidth().clickable { onOpen(product) }) {
                Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        Text(product.name, fontWeight = FontWeight.SemiBold)
                        Text("${product.brand} · ${product.presentation}", style = MaterialTheme.typography.bodySmall)
                    }
                    QuantityStepper(vm.list[product.id] ?: 1) { vm.setQuantity(product, it) }
                }
            }
        }
    }
}

@Composable
private fun Summary(c: BasketComparison) {
    val best = c.bestSingleStore
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (best != null) {
                Text("Tienda más barata para toda tu lista", style = MaterialTheme.typography.labelLarge)
                Text(
                    "${best.store.name}: ${formatCop(best.total)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Text("Ninguna tienda tiene todos los productos de tu lista.", fontWeight = FontWeight.SemiBold)
            }
            if (c.splitSavings > 0) {
                Text(
                    "Si compras cada producto donde sale más barato pagarías ${formatCop(c.splitTotal)} " +
                        "y ahorrarías ${formatCop(c.splitSavings)} (visitando varias tiendas).",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (c.unavailable.isNotEmpty()) {
                Text(
                    "Sin precio en ninguna tienda: ${c.unavailable.joinToString { it.name }}.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun StoreBasketCard(b: StoreBasket) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(b.store.name, fontWeight = FontWeight.SemiBold)
                if (b.complete) {
                    Text("Tiene todos los productos", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(
                        "Le faltan ${b.missing.size}: ${b.missing.joinToString { it.name }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Text(formatCop(b.total), fontWeight = FontWeight.Bold)
        }
    }
}
