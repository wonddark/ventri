package com.ventri.app.ui.design.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.ventri.app.ui.design.VentriShapes
import com.ventri.app.ui.design.VentriTheme
import kotlinx.coroutines.delay

class VentriSnackbarHostState {
    var message by mutableStateOf<String?>(null)
        private set

    suspend fun showSnackbar(message: String) {
        this.message = message
        delay(3000)
        this.message = null
    }

    fun dismiss() { message = null }
}

@Composable
fun rememberVentriSnackbarHostState() = remember { VentriSnackbarHostState() }

@Composable
fun VentriSnackbarHost(hostState: VentriSnackbarHostState) {
    AnimatedVisibility(
        visible = hostState.message != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
    ) {
        hostState.message?.let { msg ->
            Box(
                modifier = Modifier
                    .shadow(elevation = 4.dp, shape = VentriShapes.small)
                    .clip(VentriShapes.small)
                    .background(color = VentriTheme.colors.onBackground, shape = VentriShapes.small)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                VentriText(
                    text = msg,
                    style = VentriTheme.typography.bodySmall,
                    color = VentriTheme.colors.background,
                )
            }
        }
    }
}
