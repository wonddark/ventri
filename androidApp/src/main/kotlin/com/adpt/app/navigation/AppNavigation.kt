package com.adpt.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.adpt.app.AdptApplication
import com.adpt.app.ui.items.ItemsScreen
import com.adpt.app.ui.overview.OverviewScreen
import com.adpt.app.ui.shopping.ShoppingScreen
import com.adpt.app.ui.stock.StockScreen
import kotlinx.coroutines.flow.filterNotNull

private sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Overview : Screen("overview", "Overview", Icons.Default.AvTimer)
    data object Shopping :
        Screen("shopping", "Shopping", Icons.Default.ShoppingCart)

    data object Stock : Screen("stock", "Stock", Icons.Default.Inventory)
    data object Items : Screen("items", "Items", Icons.Default.Category)

    companion object {
        val tabs = listOf(Overview, Shopping, Stock, Items)
    }
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as AdptApplication

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

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

    Scaffold(
        modifier = modifier,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding()
                    )
                    .shadow(
                        elevation = 8.dp,
                        shape = MaterialTheme.shapes.extraLarge
                    )
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = 0.99f }
                        .blur(
                            radius = 15.dp,
                            edgeTreatment = BlurredEdgeTreatment.Unbounded
                        )
                        .background(
                            color = NavigationBarDefaults.containerColor.copy(
                                alpha = 0.7f
                            ),
                            shape = MaterialTheme.shapes.extraLarge
                        )
                )

                NavigationBar(
                    windowInsets = WindowInsets(
                        left = 0.dp,
                        right = 0.dp,
                        top = 0.dp,
                        bottom = 0.dp
                    ),
                    containerColor = Color.Transparent,
                    modifier = Modifier
                        .background(
                            color = Color.Transparent,
                        )
                        .padding(all = 0.dp)
                ) {
                    Screen.tabs.forEach { screen ->
                        val selected = currentRoute == screen.route ||
                                (screen == Screen.Items && currentRoute?.startsWith(
                                    "${screen.route}?"
                                ) == true)

                        NavigationBarItem(
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(
                                                alpha = 0.5f
                                            ) else Color.Transparent,
                                            shape = MaterialTheme.shapes.extraLarge
                                        )
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            screen.icon,
                                            contentDescription = screen.label
                                        )
                                        Text(
                                            text = screen.label,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            },
                            label = { },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(weight = 1f, fill = true)
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Overview.route,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable(Screen.Overview.route) { OverviewScreen() }
            composable(Screen.Shopping.route) { ShoppingScreen(navController = navController) }
            composable(Screen.Stock.route) { StockScreen() }
            composable(
                route = "${Screen.Items.route}?selectionMode={selectionMode}",
                arguments = listOf(
                    navArgument("selectionMode") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                ),
            ) { ItemsScreen(navController = navController) }
        }
    }
}
