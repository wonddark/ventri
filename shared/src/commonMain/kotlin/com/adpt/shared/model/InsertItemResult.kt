package com.ventri.shared.model

sealed class InsertItemResult {
    data class Success(val id: String) : InsertItemResult()
    data object DuplicateName : InsertItemResult()
}
