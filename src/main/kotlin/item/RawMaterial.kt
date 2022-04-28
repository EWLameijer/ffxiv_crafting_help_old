package item

import Category.*

class RawMaterial(name: String, level: Int, category: GatheringCategory, private val location: String?) :
    Item(name, category, level) {

    companion object {
        fun parse(input: String, category: GatheringCategory): RawMaterial {
            val dataWithPossibleLocation = input.split(":")
            val location = if (dataWithPossibleLocation.size == 1) null else dataWithPossibleLocation[1].trim()
            val components = dataWithPossibleLocation[0].trim().split(" ")
            val level = components[0].toInt()
            val furtherComponents = components.drop(1)
            val nameWithAt = furtherComponents.joinToString(" ").trim()

            return RawMaterial(nameWithAt.drop(1), level, category, location)
        }
    }

    override fun analyze() {
        println("analyzing a raw material")
    }

    override fun toString() = "$name ($category $level)" + if (location != null) " - found at $location" else ""
}