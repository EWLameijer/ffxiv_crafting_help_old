package item

import Category
import Category.*

sealed class Source(val level: Int, val manner: Category) {
    override fun toString() = "$manner $level"
}

class Gathering(level: Int, manner: GatheringCategory) : Source(level, manner)

class Crafting(level: Int, manner: CraftingCategory) : Source(level, manner)