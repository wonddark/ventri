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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.adpt.app.AdptApplication
import com.adpt.app.ui.design.AdptTheme
import com.adpt.app.ui.design.LocalBarsVisible
import com.adpt.app.ui.design.LocalNavBarHeight
import com.adpt.app.ui.design.components.AdptNavBar
import com.adpt.app.ui.design.components.AdptNavItem
import com.adpt.app.ui.items.ItemsScreen
import com.adpt.app.ui.overview.OverviewScreen
import com.adpt.app.ui.preferences.PreferencesScreen
import com.adpt.app.ui.shopping.ShoppingScreen
import com.adpt.app.ui.stock.StockScreen
import kotlinx.coroutines.flow.filterNotNull

private sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Overview : Screen("overview", "Overview", Icons.Default.AvTimer)
    data object Shopping : Screen("shopping", "Refills", Icons.Default.ShoppingCart)
    data object Stock : Screen("stock", "Stock", Icons.Default.Inventory)
    data object Items : Screen("items", "Items", Icons.Default.Category)

    companion object {
        val tabs = listOf(Overview, Shopping, Stock, Items)
    }
}

private val navItems = Screen.tabs.map { screen ->
    AdptNavItem(route = screen.route, label = screen.label, icon = screen.icon)
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as AdptApplication
    val density = LocalDensity.current

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val selectionModeActive = backStackEntry?.arguments?.getBoolean("selectionMode") ?: false
    val isTopLevelRoute = Screen.tabs.any { screen ->
        currentRoute == screen.route ||
            (screen == Screen.Items &&
                currentRoute?.startsWith("${screen.route}?") == true &&
                !selectionModeActive)
    }

    var navBarHeightPx by remember { mutableIntStateOf(0) }
    val navBarHeightDp = with(density) { navBarHeightPx.toDp() }

    LaunchedEffect(Unit) {
        app.pendingNavTarget.filterNotNull().collect { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            app.pendingNavTarget.value = null
        }
    }

    CompositionLocalProvider(
        LocalNavBarHeight provides navBarHeightDp,
        LocalBarsVisible provides true,
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AdptTheme.colors.background),
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Overview.route,
            ) {
                composable(Screen.Overview.route) {
                    OverviewScreen(
                        onOpenSettings = { navController.navigate("preferences") },
                        onAddItem = { navController.navigate("items?add=true") },
                    )
                }
                composable(Screen.Shopping.route) {
                    ShoppingScreen(navController = navController)
                }
                composable(Screen.Stock.route) {
                    StockScreen()
                }
                composable("preferences") {
                    PreferencesScreen(onNavigateUp = { navController.navigateUp() })
                }
                composable(
                    route = "${Screen.Items.route}?selectionMode={selectionMode}&add={add}",
                    arguments = listOf(
                        navArgument("selectionMode") {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                        navArgument("add") {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                    ),
                ) { ItemsScreen(navController = navController) }
            }

            if (isTopLevelRoute) {
                AdptNavBar(
                    items = navItems,
                    currentRoute = currentRoute,
                    onItemSelected = { item ->
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { navBarHeightPx = it.height },
                )
            }
        }
    }
}
