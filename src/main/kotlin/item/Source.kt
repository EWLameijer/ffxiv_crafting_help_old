package item

import Category
import Category.*

import recipes
import wishList

sealed class Source(val level: Int, val manner: Category) {
    abstract fun analyze(item: Item)
    override fun toString() = "$manner $level"
    abstract val typeString : String
    fun describe() = "$typeString ($this)"
}

class Gathering(level: Int, manner: GatheringCategory, val location: String?) : Source(level, manner) {
    override fun analyze(item: Item) {}
    override val typeString = "raw material"
}

class Crafting(level: Int, manner: CraftingCategory, var recipe: Recipe?) : Source(level, manner) {
    override val typeString = "crafted item"

    fun recipe(itemName: String) : Recipe {
        if (recipe == null) recipe = Recipe.obtainFromUser(itemName)
        return recipe!!
    }

    override fun analyze(item: Item) {
        if (recipe != null) println(recipe)
        else {
            recipe = Recipe.obtainFromUser(item.name)
            recipes += item
        }
        println("Put on wish list?")
        val reply = readln()
        if (reply.uppercase().startsWith("Y")) {
            wishList += item.name
            recipe!!.checkMaterialAvailability()
        }
    }
}