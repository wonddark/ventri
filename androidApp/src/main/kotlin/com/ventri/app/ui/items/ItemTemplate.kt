package com.ventri.app.ui.items

import androidx.annotation.StringRes
import com.ventri.app.R
import com.ventri.shared.model.ItemPriority
import com.ventri.shared.model.ItemUnit

data class ItemTemplate(
    val name: String,
    val unit: ItemUnit,
    val priority: ItemPriority,
    val consumptionRate: Double?,
)

data class ItemTemplateCategory(
    @StringRes val nameRes: Int,
    val templates: List<ItemTemplate>,
)

fun ItemTemplate.toPrefill(): ItemFormPrefill = ItemFormPrefill(
    name = name,
    unit = unit,
    priority = priority,
    consumptionRate = consumptionRate,
)

private fun template(
    name: String,
    unit: ItemUnit,
    priority: ItemPriority = ItemPriority.Normal,
): ItemTemplate = ItemTemplate(
    name = name,
    unit = unit,
    priority = priority,
    consumptionRate = if (unit == ItemUnit.PIECE) 1.0 else null,
)

val itemTemplateCategories: List<ItemTemplateCategory> = listOf(
    ItemTemplateCategory(
        nameRes = R.string.items_template_category_food_pantry,
        templates = listOf(
            template("Bread", ItemUnit.PIECE),
            template("Eggs", ItemUnit.PIECE),
            template("Rice", ItemUnit.KG),
            template("Pasta", ItemUnit.PACK),
            template("Flour", ItemUnit.KG),
            template("Sugar", ItemUnit.KG),
            template("Salt", ItemUnit.G),
            template("Cooking Oil", ItemUnit.L),
            template("Coffee", ItemUnit.G),
            template("Tea", ItemUnit.PACK),
            template("Honey", ItemUnit.G),
            template("Canned Tomatoes", ItemUnit.BOX),
        ),
    ),
    ItemTemplateCategory(
        nameRes = R.string.items_template_category_dairy,
        templates = listOf(
            template("Milk", ItemUnit.L),
            template("Butter", ItemUnit.G),
            template("Cheese", ItemUnit.G),
            template("Yogurt", ItemUnit.PIECE),
            template("Cream", ItemUnit.ML),
        ),
    ),
    ItemTemplateCategory(
        nameRes = R.string.items_template_category_beverages,
        templates = listOf(
            template("Water", ItemUnit.BOTTLE),
            template("Juice", ItemUnit.L),
            template("Soda", ItemUnit.BOTTLE),
            template("Beer", ItemUnit.BOTTLE),
            template("Wine", ItemUnit.BOTTLE),
        ),
    ),
    ItemTemplateCategory(
        nameRes = R.string.items_template_category_fruits_vegetables,
        templates = listOf(
            template("Apples", ItemUnit.KG),
            template("Bananas", ItemUnit.KG),
            template("Tomatoes", ItemUnit.KG),
            template("Onions", ItemUnit.KG),
            template("Potatoes", ItemUnit.KG),
            template("Carrots", ItemUnit.KG),
            template("Lemons", ItemUnit.PIECE),
        ),
    ),
    ItemTemplateCategory(
        nameRes = R.string.items_template_category_meat_fish,
        templates = listOf(
            template("Chicken", ItemUnit.KG),
            template("Beef", ItemUnit.KG),
            template("Pork", ItemUnit.KG),
            template("Fish", ItemUnit.KG),
            template("Canned Tuna", ItemUnit.BOX),
        ),
    ),
    ItemTemplateCategory(
        nameRes = R.string.items_template_category_cleaning,
        templates = listOf(
            template("Dish Soap", ItemUnit.ML),
            template("Laundry Detergent", ItemUnit.G),
            template("All-Purpose Cleaner", ItemUnit.ML),
            template("Bleach", ItemUnit.ML),
            template("Trash Bags", ItemUnit.PACK),
            template("Sponges", ItemUnit.PIECE),
        ),
    ),
    ItemTemplateCategory(
        nameRes = R.string.items_template_category_personal_care,
        templates = listOf(
            template("Shampoo", ItemUnit.ML),
            template("Conditioner", ItemUnit.ML),
            template("Toothpaste", ItemUnit.PIECE),
            template("Toothbrush", ItemUnit.PIECE),
            template("Soap", ItemUnit.PIECE),
            template("Toilet Paper", ItemUnit.PACK),
            template("Deodorant", ItemUnit.PIECE),
            template("Razor", ItemUnit.PIECE),
        ),
    ),
)
