package com.adpt.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.adpt.app.AdptApplication
import com.adpt.app.ui.design.AdptTheme
import com.adpt.app.ui.design.components.AdptNavBar
import com.adpt.app.ui.design.components.AdptNavItem
import com.adpt.app.ui.items.ItemsScreen
import com.adpt.app.ui.overview.OverviewScreen
import com.adpt.app.ui.shopping.ShoppingScreen
import com.adpt.app.ui.stock.StockScreen
import kotlinx.coroutines.flow.filterNotNull

private val navItems = listOf(
    AdptNavItem("overview", "Overview", Icons.Default.AvTimer),
    AdptNavItem("shopping", "Shopping", Icons.Default.ShoppingCart),
    AdptNavItem("stock", "Stock", Icons.Default.Inventory),
    AdptNavItem("items", "Items", Icons.Default.Category),
)

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as AdptApplication
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        app.pendingNavTarget.filterNotNull().collect { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            app.pendingNavTarget.value = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AdptTheme.colors.background),
    ) {
        NavHost(
            navController = navController,
            startDestination = "overview",
            modifier = Modifier.fillMaxSize(),
        ) {
            composable("overview") { OverviewScreen() }
            composable("shopping") { ShoppingScreen(navController = navController) }
            composable("stock") { StockScreen() }
            composable(
                route = "items?selectionMode={selectionMode}",
                arguments = listOf(
                    navArgument("selectionMode") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                ),
            ) { ItemsScreen(navController = navController) }
        }

        AdptNavBar(
            items = navItems,
            currentRoute = navItems.firstOrNull { item ->
                currentRoute == item.route ||
                        (item.route == "items" && currentRoute?.startsWith("items?") == true)
            }?.route,
            onItemSelected = { item ->
                navController.navigate(item.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
