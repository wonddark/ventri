package com.adpt.app.ui.design.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adpt.app.ui.design.AdptTheme

@Composable
fun AdptExposedDropdown(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedText: String,
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        AdptTextField(
            value = selectedText,
            onValueChange = {},
            label = label,
            readOnly = true,
            trailingIcon = {
                AdptIconButton(onClick = { onExpandedChange(!expanded) }) {
                    AdptIcon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = AdptTheme.colors.onSurface.copy(alpha = 0.6f),
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth(),
        )
        AdptDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            content()
        }
    }
}
