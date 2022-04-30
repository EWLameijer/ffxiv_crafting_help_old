import item.CraftedItem
import item.Crafting
import item.Item
import java.io.File

class NeededMaterialsCatalogue {
    fun neededSoFar(itemName: String): Int {
        if (itemName !in basicMaterials.keys) basicMaterials[itemName] = 0
        return basicMaterials[itemName]!!
    }

    fun reserveOf(amount: Int, itemName: String) {
        basicMaterials[itemName] = basicMaterials[itemName]!! + amount
    }

    fun addToOverflowList(overflow: Int, material: Item) {
        val materialName = material.name
        if (materialName !in overflowList.keys) overflowList[materialName] = 0
        overflowList[materialName] = overflowList[materialName]!! + overflow
    }

    fun saveToFile(fileName: String) {
        overflowList.forEach { (itemName, neededAmount) ->
            val item = items.find { it.name == itemName }!!
            val recipe = (item as CraftedItem).recipe()
            val quantityProduced = recipe.quantityProduced
            val minTimesToPerformRecipe = neededAmount / quantityProduced
            if (minTimesToPerformRecipe != 0) recipe.ingredients.forEach {
                search(it.second, minTimesToPerformRecipe * it.first)
            }
            // TODO
            val remainingAmount = neededAmount % quantityProduced
            val overflow = remainingAmount % quantityProduced
            if (minTimesToPerformRecipe != 0) recipe.ingredients.forEach { search(it.second, it.first) }
        }

        File(fileName).writeText(basicMaterials.map { (k, v) -> "$v $k" }.joinToString("\n"))
    }

    private val overflowList = mutableMapOf<String, Int>()

    private val basicMaterials = mutableMapOf<String, Int>()
}