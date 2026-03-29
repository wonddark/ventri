package com.adpt.shared.db

import app.cash.sqldelight.ColumnAdapter
import com.adpt.shared.model.ItemUnit

private val itemUnitAdapter = object : ColumnAdapter<ItemUnit, String> {
    override fun decode(databaseValue: String): ItemUnit = ItemUnit.valueOf(databaseValue)
    override fun encode(value: ItemUnit): String = value.name
}

fun createDatabase(driverFactory: DatabaseDriverFactory): AdptDatabase {
    val driver = driverFactory.createDriver()
    return AdptDatabase(
        driver = driver,
        ItemAdapter = Item.Adapter(unitAdapter = itemUnitAdapter)
    )
}
