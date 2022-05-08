package item

import Category
import Category.*

sealed class Source(private val level: Int, val manner: Category) {
    override fun toString() = "$manner $level"
    abstract val typeString: String
    fun describe() = "$typeString ($this)"
}

class Gathering(level: Int, manner: GatheringCategory) : Source(level, manner) {
    override val typeString = "raw material"
}

class Crafting(level: Int, manner: CraftingCategory) : Source(level, manner) {
    override val typeString = "crafted item"
}