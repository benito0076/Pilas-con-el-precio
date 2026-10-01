package co.pilas.precio.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private const val ROUTE_SEARCH = "search"
private const val ROUTE_LIST = "list"
private const val ROUTE_PRODUCT = "product/{id}"

@Composable
fun PilasApp(vm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    val currentRoute by nav.currentBackStackEntryAsState()
    val route = currentRoute?.destination?.route
    val showBottomBar = route == ROUTE_SEARCH || route == ROUTE_LIST

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == ROUTE_SEARCH,
                        onClick = { nav.goTopLevel(ROUTE_SEARCH) },
                        icon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text("Buscar") },
                    )
                    NavigationBarItem(
                        selected = route == ROUTE_LIST,
                        onClick = { nav.goTopLevel(ROUTE_LIST) },
                        icon = {
                            BadgedBox(badge = {
                                if (vm.list.isNotEmpty()) Badge { Text("${vm.list.size}") }
                            }) { Icon(Icons.Default.ShoppingCart, contentDescription = null) }
                        },
                        label = { Text("Mi lista") },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = ROUTE_SEARCH, modifier = Modifier.padding(padding)) {
            composable(ROUTE_SEARCH) {
                SearchScreen(vm, onOpen = { nav.navigate("product/${it.id}") })
            }
            composable(ROUTE_LIST) {
                ListScreen(vm, onOpen = { nav.navigate("product/${it.id}") })
            }
            composable(ROUTE_PRODUCT) { entry ->
                val product = entry.arguments?.getString("id")?.let { vm.repo.product(it) }
                if (product != null) ProductScreen(vm, product, onBack = { nav.popBackStack() })
            }
        }
    }
}

private fun NavHostController.goTopLevel(route: String) {
    navigate(route) {
        popUpTo(ROUTE_SEARCH) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
