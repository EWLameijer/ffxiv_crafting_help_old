import java.io.File
import Category.*
import Item.CraftedItem
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
        if (File(fileName).exists()) {
            recipes[category] = File(fileName).readLines().map {
                val newItem = CraftedItem.parse(it)
                // replace recipless item by item with correct recipe
                items.removeIf { it.name == newItem.name }
                items.add(newItem)
                newItem
            }.toMutableList()
        }
    }
}

fun filenameFromCategory(category: CraftingCategory) = "Recipes\\$category.txt"

fun saveAndExit() {
    saveCraftingRecipes()
    File("unknown.txt").writeText(unknowns.joinToString("\n"))
    mapToFile(inventory, "current_inventory.txt", KeyOrder.KeyLast)
    mapToFile(meaning, "abbreviations.txt", KeyOrder.KeyFirst)
    File("wishlist.txt").writeText(wishList.joinToString("\n"))
    mapToFile(neededMaterials, "needed.txt", KeyOrder.KeyLast)
    exitProcess(0)
}

private fun saveCraftingRecipes() {
    CraftingCategory.values().forEach { category ->
        val currentRecipes = recipes[category]
        if (currentRecipes != null) {
            val sortedRecipes =
                currentRecipes.distinctBy { it.name }.sortedWith(compareBy<CraftedItem> { it.level }.thenBy { it.name })
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