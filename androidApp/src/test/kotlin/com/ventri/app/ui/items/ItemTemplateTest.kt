package com.ventri.app.ui.items

import com.ventri.shared.model.ItemPriority
import com.ventri.shared.model.ItemUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ItemTemplateTest {

    @Test
    fun `all PIECE unit templates have consumptionRate of 1 0`() {
        itemTemplateCategories
            .flatMap { it.templates }
            .filter { it.unit == ItemUnit.PIECE }
            .forEach { template ->
                assertEquals(
                    "Template '${template.name}' has PIECE unit but consumptionRate != 1.0",
                    1.0,
                    template.consumptionRate!!,
                    0.0,
                )
            }
    }

    @Test
    fun `all non-PIECE unit templates have null consumptionRate`() {
        itemTemplateCategories
            .flatMap { it.templates }
            .filter { it.unit != ItemUnit.PIECE }
            .forEach { template ->
                assertNull(
                    "Template '${template.name}' has non-PIECE unit but consumptionRate is not null",
                    template.consumptionRate,
                )
            }
    }

    @Test
    fun `template list has 7 categories and 48 templates`() {
        assertEquals(7, itemTemplateCategories.size)
        assertEquals(48, itemTemplateCategories.sumOf { it.templates.size })
    }

    @Test
    fun `toPrefill maps all fields correctly`() {
        val template = ItemTemplate(
            name = "Rice",
            unit = ItemUnit.KG,
            priority = ItemPriority.Normal,
            consumptionRate = null,
        )
        val prefill = template.toPrefill()
        assertEquals("Rice", prefill.name)
        assertEquals(ItemUnit.KG, prefill.unit)
        assertEquals(ItemPriority.Normal, prefill.priority)
        assertNull(prefill.consumptionRate)
    }

    @Test
    fun `toPrefill preserves consumptionRate for PIECE templates`() {
        val template = ItemTemplate(
            name = "Bread",
            unit = ItemUnit.PIECE,
            priority = ItemPriority.Normal,
            consumptionRate = 1.0,
        )
        val prefill = template.toPrefill()
        assertEquals(1.0, prefill.consumptionRate!!, 0.0)
    }
}
