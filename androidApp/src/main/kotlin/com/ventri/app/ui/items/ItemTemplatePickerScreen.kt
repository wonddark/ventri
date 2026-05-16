package com.ventri.app.ui.items

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ventri.app.R
import com.ventri.app.ui.design.VentriTheme
import com.ventri.app.ui.design.components.VentriClickableCard
import com.ventri.app.ui.design.components.VentriIcon
import com.ventri.app.ui.design.components.VentriIconButton
import com.ventri.app.ui.design.components.VentriText
import com.ventri.app.ui.design.components.VentriTextField
import com.ventri.app.ui.design.components.VentriTopBar
import com.ventri.app.ui.util.displayName

@Composable
fun ItemTemplatePickerScreen(
    onTemplateSelected: (ItemTemplate) -> Unit,
    onStartFromScratch: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    var searchQuery by rememberSaveable { mutableStateOf("") }
    val density = LocalDensity.current
    var topAreaHeightPx by remember { mutableIntStateOf(0) }
    val topAreaHeightDp = with(density) { topAreaHeightPx.toDp() }

    val filteredCategories by remember {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                itemTemplateCategories
            } else {
                itemTemplateCategories.mapNotNull { category ->
                    val matched = category.templates.filter {
                        it.name.contains(searchQuery, ignoreCase = true)
                    }
                    if (matched.isEmpty()) null else category.copy(templates = matched)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VentriTheme.colors.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = topAreaHeightDp + 8.dp,
                bottom = 16.dp,
                start = 16.dp,
                end = 16.dp,
            ),
        ) {
            item(key = "start_from_scratch") {
                StartFromScratchCard(
                    onClick = onStartFromScratch,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (filteredCategories.isEmpty()) {
                item(key = "no_results") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        VentriText(
                            text = stringResource(R.string.items_template_no_results),
                            style = VentriTheme.typography.bodyMedium,
                            color = VentriTheme.colors.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }
            } else {
                filteredCategories.forEach { category ->
                    item(key = "header_${category.nameRes}") {
                        VentriText(
                            text = stringResource(category.nameRes),
                            style = VentriTheme.typography.labelSmall,
                            color = VentriTheme.colors.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(
                        items = category.templates,
                        key = { "${category.nameRes}_${it.name}" },
                    ) { template ->
                        TemplateRow(
                            template = template,
                            onClick = { onTemplateSelected(template) },
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VentriTheme.colors.background)
                .onSizeChanged { topAreaHeightPx = it.height },
        ) {
            VentriTopBar(
                title = {
                    VentriText(
                        text = stringResource(R.string.items_add_title),
                        style = VentriTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    VentriIconButton(onClick = onDismiss) {
                        VentriIcon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.items_template_back_cd),
                        )
                    }
                },
            )
            VentriTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = stringResource(R.string.items_template_picker_search_placeholder),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun StartFromScratchCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VentriClickableCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VentriIcon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = VentriTheme.colors.accent,
            )
            Column {
                VentriText(
                    text = stringResource(R.string.items_template_start_from_scratch),
                    style = VentriTheme.typography.titleSmall,
                )
                VentriText(
                    text = stringResource(R.string.items_template_start_from_scratch_subtitle),
                    style = VentriTheme.typography.bodySmall,
                    color = VentriTheme.colors.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun TemplateRow(
    template: ItemTemplate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VentriClickableCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                VentriText(
                    text = template.name,
                    style = VentriTheme.typography.titleSmall,
                )
                VentriText(
                    text = if (template.consumptionRate != null) {
                        stringResource(
                            R.string.items_unit_rate,
                            template.unit.displayName(),
                            template.consumptionRate.toString(),
                        )
                    } else {
                        template.unit.displayName()
                    },
                    style = VentriTheme.typography.bodySmall,
                    color = VentriTheme.colors.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}
