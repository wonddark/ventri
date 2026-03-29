package com.adpt.shared.model

sealed class InsertItemResult {
    data class Success(val id: String) : InsertItemResult()
    data object DuplicateName : InsertItemResult()
}
