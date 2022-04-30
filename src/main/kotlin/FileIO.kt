import java.io.File
import Category.*
import item.Item
import kotlin.system.exitProcess

fun loadUnknowns() {
    unknowns += File("unknown.txt").readLines()
}

// ii iron ingot => map[1] = rest
fun loadAbbreviations(): Map<String, String> = File("abbreviations.txt").readLines().associate {
    val lineItems = it.split(" ")
    val abbreviation = lineItems[0].trim()
    val itemName = lineItems.drop(1).joinToString(" ")
    abbreviation to itemName
}

// 1 iron ingot => map[rest] = 1
fun loadItemsWithCounts(filename: String): Map<String, Int> = File(filename).readLines().associate {
    val lineItems = it.split(" ")
    val amount = lineItems[0].trim().toInt()
    val itemName = lineItems.drop(1).joinToString(" ")
    itemName to amount
}

fun loadRecipes() {
    CraftingCategory.values().forEach { category ->
        val fileName = filenameFromCategory(category)
        if (File(fileName).exists()) recipes += File(fileName).readLines().map { Item.parseFromRecipeFile(it) }
    }
}

fun filenameFromCategory(category: CraftingCategory) = "Recipes\\$category.txt"

fun saveAndExit() {
    saveCraftingRecipes()
    File("unknown.txt").writeText(unknowns.joinToString("\n"))
    mapToFile(inventory, "current_inventory.txt", KeyOrder.KeyLast)
    mapToFile(meaning, "abbreviations.txt", KeyOrder.KeyFirst)
    File("wishlist.txt").writeText(wishList.joinToString("\n"))
    neededMaterials.saveToFile("needed.txt")
    exitProcess(0)
}

private fun saveCraftingRecipes() {
    CraftingCategory.values().forEach { category ->
        val currentRecipes = recipes.filter { it.source.manner == category }
        if (currentRecipes.isNotEmpty()) {
            val sortedRecipes = currentRecipes.sortedWith(compareBy<Item> { it.source.level }.thenBy { it.name })
            File(filenameFromCategory(category)).writeText(sortedRecipes.joinToString("\n"))
        }
    }
}

enum class KeyOrder { KeyFirst, KeyLast }

private fun <T> mapToFile(map: Map<String, T>, fileName: String, keyOrder: KeyOrder) {
    File(fileName).writeText(map.keys.sorted().joinToString("\n") {
        if (keyOrder == KeyOrder.KeyFirst) "$it ${map[it]}" else "${map[it]} $it"
    })
}