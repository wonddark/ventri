package com.ventri.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ventri.app.R
import com.ventri.shared.model.ItemPriority
import com.ventri.shared.model.ItemUnit

@Composable
fun ItemUnit.displayName(): String = stringResource(
    when (this) {
        ItemUnit.PIECE -> R.string.unit_piece
        ItemUnit.KG -> R.string.unit_kg
        ItemUnit.G -> R.string.unit_g
        ItemUnit.L -> R.string.unit_l
        ItemUnit.ML -> R.string.unit_ml
        ItemUnit.BOTTLE -> R.string.unit_bottle
        ItemUnit.BOX -> R.string.unit_box
        ItemUnit.PACK -> R.string.unit_pack
    }
)

@Composable
fun ItemPriority.displayName(): String = stringResource(
    when (this) {
        ItemPriority.Highest -> R.string.priority_highest
        ItemPriority.High -> R.string.priority_high
        ItemPriority.Normal -> R.string.priority_normal
        ItemPriority.Low -> R.string.priority_low
        ItemPriority.Lowest -> R.string.priority_lowest
    }
)
