package com.adpt.shared.db

import app.cash.sqldelight.ColumnAdapter
import com.adpt.shared.model.ItemPriority
import com.adpt.shared.model.ItemUnit
import com.adpt.shared.model.ShoppingListStatus

private val itemUnitAdapter = object : ColumnAdapter<ItemUnit, String> {
    override fun decode(databaseValue: String): ItemUnit = ItemUnit.valueOf(databaseValue)
    override fun encode(value: ItemUnit): String = value.name
}

private val itemPriorityAdapter = object : ColumnAdapter<ItemPriority, String> {
    override fun decode(databaseValue: String): ItemPriority = ItemPriority.valueOf(databaseValue)
    override fun encode(value: ItemPriority): String = value.name
}

private val shoppingListStatusAdapter = object : ColumnAdapter<ShoppingListStatus, String> {
    override fun decode(databaseValue: String): ShoppingListStatus = ShoppingListStatus.valueOf(databaseValue)
    override fun encode(value: ShoppingListStatus): String = value.name
}

fun createDatabase(driverFactory: DatabaseDriverFactory): AdptDatabase {
    val driver = driverFactory.createDriver()
    return AdptDatabase(
        driver = driver,
        ItemAdapter = Item.Adapter(unitAdapter = itemUnitAdapter, priorityAdapter = itemPriorityAdapter),
        ShoppingListEntryAdapter = ShoppingListEntry.Adapter(statusAdapter = shoppingListStatusAdapter)
    )
}
