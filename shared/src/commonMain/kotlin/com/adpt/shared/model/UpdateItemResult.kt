package com.adpt.shared.model

sealed class UpdateItemResult {
    data object Success : UpdateItemResult()
    data object DuplicateName : UpdateItemResult()
}
