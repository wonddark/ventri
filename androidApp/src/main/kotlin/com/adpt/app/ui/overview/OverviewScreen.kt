package com.ventri.app.ui.overview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ventri.app.ui.components.AnimatedListItem
import com.ventri.app.ui.design.VentriShapes
import com.ventri.app.ui.design.VentriTheme
import com.ventri.app.ui.design.LocalBarsVisible
import com.ventri.app.ui.design.LocalNavBarHeight
import com.ventri.app.ui.design.components.VentriButton
import com.ventri.app.ui.design.components.VentriCard
import com.ventri.app.ui.design.components.VentriFab
import com.ventri.app.ui.design.components.VentriIcon
import com.ventri.app.ui.design.components.VentriIconButton
import com.ventri.app.ui.design.components.VentriProgressIndicator
import com.ventri.app.ui.design.components.VentriSnackbarHost
import com.ventri.app.ui.design.components.VentriSurface
import com.ventri.app.ui.design.components.VentriText
import com.ventri.app.ui.design.components.VentriTopBar
import com.ventri.app.ui.design.components.rememberVentriSnackbarHostState
import com.ventri.app.ui.design.components.ripple
import com.ventri.shared.model.Severity
import kotlin.math.abs

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

@Composable
fun OverviewScreen(
    onOpenSettings: () -> Unit,
    onAddItem: () -> Unit,
    viewModel: OverviewViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberVentriSnackbarHostState()
    val navBarHeight = LocalNavBarHeight.current
    val barsVisible = LocalBarsVisible.current
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        viewModel.errors.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val successItems = (uiState as? OverviewUiState.Success)?.items
    val colors = VentriTheme.colors

    var topBarHeightPx by remember { mutableIntStateOf(0) }
    val topBarHeightDp = with(density) { topBarHeightPx.toDp() }

    val listState = rememberLazyListState()
    val headerAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex != 0) {
                0f
            } else {
                val headerInfo = listState.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.key == "expanded_header" }
                    ?: return@derivedStateOf 0f
                val headerSize = headerInfo.size.toFloat()
                if (headerSize == 0f) return@derivedStateOf 0f
                (1f - listState.firstVisibleItemScrollOffset.toFloat() / headerSize)
                    .coerceIn(0f, 1f)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        when (val state = uiState) {
            OverviewUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(top = topBarHeightDp),
                contentAlignment = Alignment.Center,
            ) {
                VentriProgressIndicator()
            }

            is OverviewUiState.Success -> if (state.criticalCount == 0 && state.highCount == 0) {
                if (!state.hasAnyItems) {
                    OverviewGetStartedState(
                        topPadding = topBarHeightDp,
                        bottomPadding = navBarHeight + 16.dp,
                        onAddItem = onAddItem,
                    )
                } else {
                    OverviewAllGoodState(
                        topPadding = topBarHeightDp,
                        bottomPadding = navBarHeight,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = topBarHeightDp,
                        bottom = navBarHeight + 72.dp,
                        start = 16.dp,
                        end = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "expanded_header") {
                        val criticalSelected = state.severityFilter == Severity.Critical
                        val highSelected = state.severityFilter == Severity.High
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(headerAlpha)
                                .padding(top = 36.dp, bottom = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            VentriText(
                                text = "Overview",
                                style = VentriTheme.typography.titleLarge.copy(
                                    fontSize = 28.sp,
                                    textAlign = TextAlign.Center,
                                ),
                                color = colors.onBackground,
                            )
                            Spacer(Modifier.height(8.dp))
                            VentriText(
                                text = "Here are the items that need your attention right now.",
                                style = VentriTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                                color = colors.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth(fraction = 0.7f)
                            )
                            Spacer(Modifier.height(32.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                SummaryChip(
                                    count = state.criticalCount,
                                    label = "Critical",
                                    containerColor = if (criticalSelected) colors.critical else colors.criticalContainer,
                                    contentColor = if (criticalSelected) colors.onCritical else colors.onCriticalContainer,
                                    onClick = { viewModel.handleIntent(OverviewIntent.ToggleSeverityFilter(Severity.Critical)) },
                                )
                                SummaryChip(
                                    count = state.highCount,
                                    label = "High",
                                    containerColor = if (highSelected) colors.warning else colors.warningContainer,
                                    contentColor = if (highSelected) colors.onWarning else colors.onWarningContainer,
                                    onClick = { viewModel.handleIntent(OverviewIntent.ToggleSeverityFilter(Severity.High)) },
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            VentriButton(
                                onClick = { viewModel.addAllToShoppingList(state.items.map { it.id }) },
                                modifier = Modifier.widthIn(min = 200.dp),
                            ) {
                                VentriText("Add all to my refill list", color = colors.onAccent)
                            }
                        }
                    }

                    if (state.items.isEmpty()) {
                        item(key = "empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                VentriText(
                                    text = "No items match this filter",
                                    style = VentriTheme.typography.bodyMedium,
                                    color = colors.onSurface.copy(alpha = 0.6f),
                                )
                            }
                        }
                    } else {
                        items(state.items, key = { it.id }) { item ->
                            AnimatedListItem(
                                index = state.items.indexOf(item),
                                animationKey = state.listVersion,
                            ) {
                                OverviewItemCard(
                                    item = item,
                                    onAddToShoppingList = {
                                        viewModel.handleIntent(OverviewIntent.AddToShoppingList(item.id))
                                    },
                                    onIgnore = {
                                        viewModel.handleIntent(OverviewIntent.IgnoreItem(item.id))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pinned top bar overlay — title only appears once the expanded header scrolls away
        VentriTopBar(
            title = {
                VentriText(
                    text = "Overview",
                    style = VentriTheme.typography.titleMedium,
                    modifier = Modifier.alpha(1f - headerAlpha),
                )
            },
            actions = {
                VentriIconButton(onClick = onOpenSettings) {
                    VentriIcon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                    )
                }
                VentriIconButton(onClick = { viewModel.refresh() }) {
                    VentriIcon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                    )
                }
            },
            modifier = Modifier.onSizeChanged { topBarHeightPx = it.height },
        )

        // FAB — hidden while the expanded header (which has its own add-all button) is visible
        AnimatedVisibility(
            visible = barsVisible && !successItems.isNullOrEmpty() && headerAlpha < 0.01f,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = navBarHeight + 16.dp),
        ) {
            VentriFab(
                onClick = {
                    viewModel.addAllToShoppingList(successItems!!.map { it.id })
                },
            ) {
                VentriIcon(
                    imageVector = Icons.Default.AddShoppingCart,
                    contentDescription = null,
                    tint = VentriTheme.colors.onAccent,
                )
            }
        }

        // Snackbar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarHeight + 8.dp, start = 16.dp, end = 16.dp),
        ) {
            VentriSnackbarHost(snackbarHostState)
        }
    }
}

@Composable
private fun OverviewGetStartedState(
    topPadding: Dp,
    bottomPadding: Dp,
    onAddItem: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = topPadding, bottom = bottomPadding, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        VentriIcon(
            imageVector = Icons.Default.AvTimer,
            contentDescription = null,
            tint = VentriTheme.colors.accent,
            modifier = Modifier
                .background(VentriTheme.colors.accentMuted, shape = VentriShapes.pill)
                .padding(20.dp),
        )
        Spacer(Modifier.height(20.dp))
        VentriText(
            text = "Let's get started",
            style = VentriTheme.typography.titleLarge,
            color = VentriTheme.colors.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        VentriText(
            text = "This is your command centre. I watch everything you have at home and alert you the moment something is about to run out.",
            style = VentriTheme.typography.bodyMedium,
            color = VentriTheme.colors.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(32.dp))
        OverviewTipCard(
            icon = Icons.Default.Inventory,
            title = "I track what you have",
            body = "Once you add items and log your stock, I'll calculate how quickly you're using them and predict when you'll run out.",
        )
        Spacer(Modifier.height(12.dp))
        OverviewTipCard(
            icon = Icons.Default.AddShoppingCart,
            title = "I tell you what needs attention",
            body = "I only surface items that are running low or out of stock — nothing else. One tap and they're added to your refill list.",
        )
        Spacer(Modifier.height(32.dp))
        VentriButton(
            onClick = onAddItem,
            modifier = Modifier.fillMaxWidth(),
        ) {
            VentriText("Add my first item", color = VentriTheme.colors.onAccent)
        }
    }
}

@Composable
private fun OverviewAllGoodState(topPadding: Dp, bottomPadding: Dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding, bottom = bottomPadding, start = 24.dp, end = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            VentriIcon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = VentriTheme.colors.ok,
                modifier = Modifier
                    .background(VentriTheme.colors.ok.copy(alpha = 0.12f), shape = VentriShapes.pill)
                    .padding(20.dp),
            )
            Spacer(Modifier.height(20.dp))
            VentriText(
                text = "You're all stocked up",
                style = VentriTheme.typography.titleLarge,
                color = VentriTheme.colors.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            VentriText(
                text = "Nothing needs your attention right now. I'll let you know as soon as something starts running low.",
                style = VentriTheme.typography.bodyMedium,
                color = VentriTheme.colors.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun OverviewTipCard(icon: ImageVector, title: String, body: String) {
    VentriCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            VentriIcon(
                imageVector = icon,
                contentDescription = null,
                tint = VentriTheme.colors.accent,
                modifier = Modifier
                    .background(VentriTheme.colors.accentMuted, shape = VentriShapes.small)
                    .padding(8.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                VentriText(title, style = VentriTheme.typography.titleSmall)
                VentriText(
                    body,
                    style = VentriTheme.typography.bodySmall,
                    color = VentriTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun SummaryChip(
    count: Int,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(VentriShapes.card)
            .background(color = containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            VentriText(
                text = count.toString(),
                style = VentriTheme.typography.labelMedium,
                color = contentColor,
            )
            Spacer(modifier = Modifier.width(4.dp))
            VentriText(
                text = label,
                style = VentriTheme.typography.labelMedium,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun OverviewItemCard(
    item: OverviewItemUiModel,
    onAddToShoppingList: () -> Unit,
    onIgnore: () -> Unit,
) {
    val colors = VentriTheme.colors
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { 100.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    var cardWidth by remember { mutableIntStateOf(0) }

    // Reset offset if isInShoppingList flips (e.g. after swiping right)
    LaunchedEffect(item.isInShoppingList) { offsetX.snapTo(0f) }

    val accentColor = when (item.severity) {
        Severity.Critical -> colors.critical
        Severity.High -> colors.warning
        Severity.Normal, Severity.Low -> colors.ok
    }

    Box(modifier = Modifier.fillMaxWidth().onSizeChanged { cardWidth = it.width }) {
        // Background reveal — clipped to card shape so corners stay rounded
        Box(modifier = Modifier.matchParentSize().clip(VentriShapes.card)) {
            // Swipe-right: add to refill list (only when not already in list)
            if (!item.isInShoppingList) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = (offsetX.value / thresholdPx).coerceIn(0f, 1f) }
                        .background(colors.ok),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    VentriIcon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(start = 24.dp)
                            .graphicsLayer {
                                val p = (offsetX.value / thresholdPx).coerceIn(0f, 1f)
                                scaleX = 0.6f + 0.4f * p
                                scaleY = scaleX
                            },
                    )
                }
            }
            // Swipe-left: ignore
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = (-offsetX.value / thresholdPx).coerceIn(0f, 1f) }
                    .background(colors.critical),
                contentAlignment = Alignment.CenterEnd,
            ) {
                VentriIcon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(end = 24.dp)
                        .graphicsLayer {
                            val p = (-offsetX.value / thresholdPx).coerceIn(0f, 1f)
                            scaleX = 0.6f + 0.4f * p
                            scaleY = scaleX
                        },
                )
            }
        }

        // Foreground card — slides with the drag gesture
        VentriCard(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(item.isInShoppingList) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                when {
                                    offsetX.value > thresholdPx && !item.isInShoppingList -> {
                                        offsetX.animateTo(
                                            cardWidth.toFloat(),
                                            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                                        )
                                        onAddToShoppingList()
                                    }
                                    offsetX.value < -thresholdPx -> {
                                        offsetX.animateTo(
                                            -cardWidth.toFloat(),
                                            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                                        )
                                        onIgnore()
                                    }
                                    else -> offsetX.animateTo(
                                        0f,
                                        spring(stiffness = Spring.StiffnessMedium),
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val maxRight = if (item.isInShoppingList) 0f else cardWidth.toFloat()
                                offsetX.snapTo(
                                    (offsetX.value + dragAmount).coerceIn(-cardWidth.toFloat(), maxRight),
                                )
                            }
                        },
                    )
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DaysBadge(deltaMillis = item.deltaMillis, color = accentColor)
                    Column(modifier = Modifier.weight(1f)) {
                        VentriText(
                            text = item.name,
                            style = VentriTheme.typography.titleMedium,
                        )
                        VentriText(
                            text = item.deltaMillis?.toDaysText() ?: "Not in stock",
                            style = VentriTheme.typography.bodySmall,
                            color = colors.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    if (item.isInShoppingList) {
                        InListBadge()
                    }
                }
            }
        }
    }
}

@Composable
private fun InListBadge() {
    val colors = VentriTheme.colors
    VentriSurface(
        color = colors.accentMuted,
        shape = VentriShapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            VentriIcon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = colors.accent,
            )
            VentriText(
                text = "In list",
                style = VentriTheme.typography.labelSmall,
                color = colors.accent,
            )
        }
    }
}

@Composable
private fun DaysBadge(deltaMillis: Long?, color: Color) {
    val label = when {
        deltaMillis == null -> "--"
        deltaMillis <= 0 -> "0d"
        else -> "${deltaMillis / MILLIS_PER_DAY}d"
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        VentriText(
            text = label,
            style = VentriTheme.typography.labelMedium,
            color = color,
        )
    }
}

private fun Long.toDaysText(): String {
    val days = abs(this / MILLIS_PER_DAY)
    return when {
        this <= 0 && days == 0L -> "Depleting today"
        this <= 0 -> "$days day(s) overdue"
        this < MILLIS_PER_DAY -> "Less than a day remaining"
        days == 1L -> "1 day remaining"
        else -> "$days days remaining"
    }
}
