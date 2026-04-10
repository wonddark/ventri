package com.adpt.app.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.adpt.app.ui.design.AdptTheme

private enum class ScaffoldSlot { TopBar, BottomBar, Fab, Snackbar, Content }

@Composable
fun AdptScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    SubcomposeLayout(modifier = modifier.background(AdptTheme.colors.background)) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)

        val topBarPlaceables = subcompose(ScaffoldSlot.TopBar, topBar).map { it.measure(loose) }
        val topBarHeight = topBarPlaceables.maxOfOrNull { it.height } ?: 0

        val bottomBarPlaceables = subcompose(ScaffoldSlot.BottomBar, bottomBar).map { it.measure(loose) }
        val bottomBarHeight = bottomBarPlaceables.maxOfOrNull { it.height } ?: 0

        val fabPlaceables = subcompose(ScaffoldSlot.Fab, floatingActionButton).map { it.measure(loose) }
        val fabHeight = fabPlaceables.maxOfOrNull { it.height } ?: 0
        val fabWidth = fabPlaceables.maxOfOrNull { it.width } ?: 0

        val snackbarPlaceables = subcompose(ScaffoldSlot.Snackbar, snackbarHost).map { it.measure(loose) }
        val snackbarHeight = snackbarPlaceables.maxOfOrNull { it.height } ?: 0
        val snackbarWidth = snackbarPlaceables.maxOfOrNull { it.width } ?: 0

        val contentPadding = PaddingValues(
            top = topBarHeight.toDp(),
            bottom = bottomBarHeight.toDp(),
        )
        val contentPlaceables = subcompose(ScaffoldSlot.Content) { content(contentPadding) }
            .map { it.measure(constraints.copy(minHeight = 0)) }

        val w = constraints.maxWidth
        val h = constraints.maxHeight

        layout(w, h) {
            contentPlaceables.forEach { it.placeRelative(0, 0) }
            topBarPlaceables.forEach { it.placeRelative(0, 0) }
            bottomBarPlaceables.forEach { it.placeRelative(0, h - bottomBarHeight) }

            val fabSpacing = 16.dp.roundToPx()
            fabPlaceables.forEach {
                it.placeRelative(
                    x = w - fabWidth - fabSpacing,
                    y = h - bottomBarHeight - fabHeight - fabSpacing,
                )
            }

            val snackbarSpacing = 8.dp.roundToPx()
            snackbarPlaceables.forEach {
                it.placeRelative(
                    x = (w - snackbarWidth) / 2,
                    y = h - bottomBarHeight - snackbarHeight - snackbarSpacing,
                )
            }
        }
    }
}
