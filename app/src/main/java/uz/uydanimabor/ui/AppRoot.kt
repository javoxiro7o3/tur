package uz.uydanimabor.ui

import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import uz.uydanimabor.MainViewModel
import uz.uydanimabor.data.UserSettings
import uz.uydanimabor.ui.screens.FavoritesScreen
import uz.uydanimabor.ui.screens.OnboardingScreen
import uz.uydanimabor.ui.screens.ProductsScreen
import uz.uydanimabor.ui.screens.RecipeScreen
import uz.uydanimabor.ui.screens.RecipesScreen
import uz.uydanimabor.ui.screens.SettingsScreen
import uz.uydanimabor.ui.screens.ShoppingScreen

object Routes {
    const val PRODUCTS = "products"
    const val RECIPES = "recipes"
    const val SHOPPING = "shopping"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val RECIPE = "recipe/{id}"
    fun recipe(id: String) = "recipe/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.PRODUCTS, "Mahsulotlar", Icons.Filled.Home),
    Tab(Routes.RECIPES, "Taomlar", Icons.Filled.List),
    Tab(Routes.SHOPPING, "Xarid roʻyxati", Icons.Filled.ShoppingCart),
    Tab(Routes.FAVORITES, "Sevimlilar", Icons.Filled.Favorite),
)

@Composable
fun AppRoot(vm: MainViewModel, settings: UserSettings) {
    if (!settings.onboardingDone) {
        // F-13: birinchi ishga tushganda tanishuv
        OnboardingScreen(onDone = vm::finishOnboarding)
        return
    }
    val nav = rememberNavController()
    val dir = androidx.compose.ui.platform.LocalLayoutDirection.current
    val snackbar = remember { SnackbarHostState() }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = tabs.any { it.route == route }
    val shopping by vm.shopping.collectAsStateWithLifecycle()
    val pending = shopping.count { !it.checked }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = {
                                if (tab.route == Routes.SHOPPING && pending > 0) {
                                    BadgedBox(badge = { Badge { Text("$pending") } }) {
                                        Icon(tab.icon, contentDescription = null)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = null)
                                }
                            },
                            label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            }
        },
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Routes.PRODUCTS,
            modifier = Modifier.padding(
                start = inner.calculateStartPadding(dir),
                end = inner.calculateEndPadding(dir),
                bottom = inner.calculateBottomPadding(),
            ),
        ) {
            composable(Routes.PRODUCTS) {
                ProductsScreen(
                    vm = vm,
                    snackbar = snackbar,
                    onShowRecipes = { nav.switchTab(Routes.RECIPES) },
                    onSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.RECIPES) {
                RecipesScreen(
                    vm = vm,
                    onOpen = { nav.navigate(Routes.recipe(it)) },
                    onGoProducts = { nav.switchTab(Routes.PRODUCTS) },
                    onSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.SHOPPING) {
                ShoppingScreen(vm = vm, snackbar = snackbar, onSettings = { nav.navigate(Routes.SETTINGS) })
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(
                    vm = vm,
                    onOpen = { nav.navigate(Routes.recipe(it)) },
                    onSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(vm = vm, settings = settings, snackbar = snackbar, onBack = { nav.popBackStack() })
            }
            composable(
                Routes.RECIPE,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                RecipeScreen(
                    vm = vm,
                    recipeId = id,
                    snackbar = snackbar,
                    onBack = { nav.popBackStack() },
                    onOpenShopping = { nav.switchTab(Routes.SHOPPING) },
                )
            }
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
