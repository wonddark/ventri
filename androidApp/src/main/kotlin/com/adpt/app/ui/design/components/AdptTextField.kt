package com.ventri.app.ui.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ventri.app.ui.design.VentriShapes
import com.ventri.app.ui.design.VentriTheme

enum class VentriTextFieldVariant { Outlined, Transparent }

@Composable
fun VentriTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = false,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    variant: VentriTextFieldVariant = VentriTextFieldVariant.Outlined,
) {
    val colors = VentriTheme.colors
    val typography = VentriTheme.typography
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val hasContent = value.isNotEmpty()

    val labelScale by animateFloatAsState(
        targetValue = if (focused || hasContent) 0.75f else 1f,
        animationSpec = tween(150),
        label = "labelScale",
    )
    @Suppress("UNUSED_VARIABLE")
    val labelOffsetY by animateFloatAsState(
        targetValue = if (focused || hasContent) -22f else 0f,
        animationSpec = tween(150),
        label = "labelOffsetY",
    )

    val borderColor = when {
        isError -> colors.critical
        focused -> colors.accent
        else -> colors.outline
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (variant == VentriTextFieldVariant.Outlined) {
                        Modifier
                            .clip(VentriShapes.small)
                            .border(1.dp, borderColor, VentriShapes.small)
                    } else Modifier
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingIcon != null) {
                    Box(modifier = Modifier.padding(end = 8.dp)) { leadingIcon() }
                }
                Box(modifier = Modifier.weight(1f)) {
                    // Floating label
                    if (label != null) {
                        VentriText(
                            text = label,
                            style = typography.bodyMedium.copy(
                                fontSize = (typography.bodyMedium.fontSize.value * labelScale).sp,
                                color = when {
                                    isError -> colors.critical
                                    focused -> colors.accent
                                    else -> colors.onSurface.copy(alpha = 0.6f)
                                },
                            ),
                            modifier = Modifier
                                .padding(top = if (label != null && variant == VentriTextFieldVariant.Outlined) 18.dp else 0.dp)
                                .then(
                                    if (focused || hasContent)
                                        Modifier.padding(top = 0.dp)
                                    else Modifier
                                ),
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = if (label != null) 24.dp else 14.dp,
                                bottom = 14.dp,
                            ),
                        textStyle = typography.bodyMedium.copy(color = colors.onSurface),
                        cursorBrush = SolidColor(colors.accent),
                        singleLine = singleLine,
                        readOnly = readOnly,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        visualTransformation = visualTransformation,
                        interactionSource = interactionSource,
                        decorationBox = { innerTextField ->
                            Box {
                                if (value.isEmpty() && placeholder != null && (label == null || focused)) {
                                    VentriText(
                                        text = placeholder,
                                        style = typography.bodyMedium,
                                        color = colors.onSurface.copy(alpha = 0.4f),
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }
                if (trailingIcon != null) {
                    Box(modifier = Modifier.padding(start = 8.dp)) { trailingIcon() }
                }
            }
        }
        if (supportingText != null) {
            VentriText(
                text = supportingText,
                style = typography.labelSmall,
                color = if (isError) colors.critical else colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
            )
        }
    }
}
