package Item

import addRecipeToRecipes
import wishList

class CraftedItem(
    name: String,
    level: Int,
    category: Category.CraftingCategory,
    private val usage: String?,
    var recipe: Recipe?
) :
    Item(name, category, level) {

    override fun analyze() {
        if (recipe != null) println(recipe)
        else {
            recipe = Recipe.obtainFromUser(name)
            addRecipeToRecipes(this)
        }
        println("Put on wish list?")
        val reply = readln()
        if (reply.uppercase().startsWith("Y")) {
            wishList += name
            recipe!!.checkMaterialAvailability()
        }
    }

    override fun toString() = "$name ($category $level) [$usage] $recipe"

    companion object {
        // example "initiate's awl (Blacksmith 23) [O23] 1 produced by 1x iron ingot, 1x yew lumber, 1x clove oil"
        fun parse(input: String): CraftedItem {
            val items = input.split(' ')
            val nameParts = items.takeWhile { it[0] != '(' } // initiate's awl
            val restStart = nameParts.size // 2
            val category = Category.CraftingCategory.valueOf(items[restStart].drop(1)) // Blacksmith
            val level = items[restStart + 1].dropLast(1).toInt() // 23
            val usage = items[restStart + 2].drop(1).dropLast(1) // O23
            val recipeAsString = items.drop(restStart + 3).joinToString(" ").trim()
            val name = nameParts.joinToString(" ")
            return CraftedItem(name, level, category, usage, Recipe.parse(recipeAsString))
        }

        fun parseFromSource(input: String, category: Category.CraftingCategory): CraftedItem {
            val components = input.trim().split(" ")
            val level = components[0].toInt()
            val furtherComponents = components.drop(1)
            val (nameWithAt, usage) = if (furtherComponents.last().any { it.isUpperCase() })
                furtherComponents.dropLast(1).joinToString(" ").trim() to furtherComponents.last().trim()
            else furtherComponents.joinToString(" ").trim() to null
            return CraftedItem(nameWithAt.drop(1), level, category, usage, null)
        }
    }
}