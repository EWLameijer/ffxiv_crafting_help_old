import java.io.File

import Category.*
import Item.CraftedItem
import Item.Item
import Item.RawMaterial
import Item.Recipe
import kotlin.math.sqrt

val items = mutableListOf<Item>()
var inventory = mutableMapOf<String, Int>()
var meaning = mutableMapOf<String, String>() // for abbreviations
var wishList = mutableListOf<String>()
var neededMaterials = NeededMaterialsCatalogue()
val recipes = mutableMapOf<CraftingCategory, MutableList<CraftedItem>>()
val unknowns = mutableListOf<String>()

fun main() {
    val lines = File("""D:\GoogleDriveEW\Hobby\Spellen\FFXIV\Lhei_Phoenix\crafting.txt""").readLines()
    val categoriesWithRawMaterials = lines.dropWhile { it != "#CRP" }.takeWhile { it != "$$" }

    items += categorizeRawMaterials(categoriesWithRawMaterials)
    items.forEach(::println)

    loadUnknowns()
    loadRecipes()
    inventory = loadItemsWithCounts("current_inventory.txt").toMutableMap()

    meaning = loadAbbreviations().toMutableMap()
    loadWishList()

    allowUserToSearch(items)
}

//example: iron ornamental hammer = initiate's chaser hammer iv ai

private fun loadWishList() {
    wishList = File("wishlist.txt").readLines().toMutableList()
    wishList.forEach { itemName ->
        val item = (items.find { it.name == itemName }!! as CraftedItem)
        if (item.recipe == null) {
            item.recipe = Recipe.obtainFromUser(item.name)
            addRecipeToRecipes(item)
        }
        item.recipe!!.ingredients.forEach { search(it.second, it.first) }
    }
}

fun search(itemName: String, amount: Int) {
    if (!inventory.containsKey(itemName)) inventory[itemName] =
        ask("How many of $itemName is in your inventory? ").toInt()
    val neededSoFar = neededMaterials.neededSoFar(itemName)
    val availableAmount = inventory[itemName]!! - neededSoFar
    println("For $itemName: $amount needed, $availableAmount available, needed for other recipes $neededSoFar")
    if (amount <= availableAmount) neededMaterials.reserveOf(amount, itemName)
    else handleMaterialShortage(itemName, amount)
}

private fun handleMaterialShortage(itemName: String, amount: Int) {
    when (val material = items.find { it.name == itemName }) {
        null ->
            if (itemName in unknowns) neededMaterials.reserveOf(amount, itemName)
            else throw IllegalArgumentException("bug in search!")
        is RawMaterial -> neededMaterials.reserveOf(amount, itemName)
        is CraftedItem -> {
            if (material.recipe == null) {
                material.recipe = Recipe.obtainFromUser(material.name)
                addRecipeToRecipes(material)
            }
            val recipe = material.recipe!!
            val quantityProduced = recipe.quantityProduced
            if (quantityProduced != 1) {
                val minTimesToPerformRecipe = amount / quantityProduced
                if (minTimesToPerformRecipe != 0) recipe.ingredients.forEach {
                    search(
                        it.second,
                        minTimesToPerformRecipe * it.first
                    )
                }
                // TODO
                val overflow = amount % quantityProduced
                if (overflow != 0) neededMaterials.addToOverflowList(overflow, material)
                //if overflow != 0, add overflow item to overflow list

                material.recipe!!.ingredients.forEach { search(it.second, amount * it.first) }
            }
        }
    }
}

fun addRecipeToRecipes(material: CraftedItem) {
    val category = material.category as CraftingCategory
    if (recipes[category] == null) recipes[category] = mutableListOf(material)
    else recipes[category]!! += material
}

private fun allowUserToSearch(knownItems: List<Item>) {
    var lastSelection = listOf<Item>()
    while (true) {
        println("Please give a term to search (or the index of an item to create); # to exit:")
        val soughtMaterial = readln()
        if (soughtMaterial == "#") saveAndExit()
        if (soughtMaterial.isNotBlank() && soughtMaterial.all { it.isDigit() }) { // select item by index
            processNumber(soughtMaterial, lastSelection)
        } else {
            lastSelection = knownItems.filter { it.name.contains(soughtMaterial) }
            lastSelection.forEachIndexed { index, material -> println("${index + 1}. $material") }
        }
    }
}

private fun processNumber(soughtMaterial: String, lastSelection: List<Item>) {
    val selectedIndex = soughtMaterial.toInt()
    if (selectedIndex in 1..lastSelection.size) analyzeItem(lastSelection[selectedIndex - 1])
    else {
        val reason =
            if (lastSelection.isEmpty()) "there are no hits" else "it is outside the allowed range of 1..${lastSelection.size}"
        println("Index $selectedIndex is invalid, as $reason.")
    }
}

private fun analyzeItem(soughtMaterial: Item) {
    println("Analyzing $soughtMaterial")
    soughtMaterial.analyze()
    val description = if (soughtMaterial is RawMaterial) "raw material" else "crafted item"
    println("$description (${soughtMaterial.category} ${soughtMaterial.level})")
}

private fun categorizeRawMaterials(categoriesWithRawMaterials: List<String>): List<Item> {
    var currentCategory: Category? = null
    val rawMaterials = mutableListOf<Item>()
    for (item in categoriesWithRawMaterials) {
        println(item)
        when (item[0]) {
            '#' -> currentCategory = categoryFrom(item.substring(1).trim())
            '-' -> continue
            else -> {
                rawMaterials += createItem(item, currentCategory!!)
            }
        }
    }
    return rawMaterials
}

fun createItem(input: String, currentCategory: Category) = when (currentCategory) {
    is GatheringCategory -> RawMaterial.parse(input, currentCategory)
    is CraftingCategory -> CraftedItem.parseFromSource(input, currentCategory)
}

