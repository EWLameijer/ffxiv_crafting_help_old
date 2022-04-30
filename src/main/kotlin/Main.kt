import java.io.File

import item.*

typealias ItemName = String

val items = mutableListOf<Item>()
var inventory = mutableMapOf<ItemName, Int>()
var meaning = mutableMapOf<ItemName, String>() // for abbreviations
var wishList = mutableListOf<ItemName>()
var neededMaterials = NeededMaterialsCatalogue()
val recipes = mutableSetOf<Item>()
val unknowns = mutableListOf<ItemName>()

fun main() {
    val lines = File("""D:\GoogleDriveEW\Hobby\Spellen\FFXIV\Lhei_Phoenix\crafting.txt""").readLines()
    val categoriesWithRawMaterials = lines.dropWhile { it != "#CRP" }.takeWhile { it != "$$" }

    items += categorizeMaterials(categoriesWithRawMaterials)
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
        val item = (items.find { it.name == itemName }!!)
        if (item is CraftedItem) {
            val recipe = item.recipe()
            recipes += item
            recipe.ingredients.forEach { search(it.second, it.first) }
        }
    }
}

fun search(itemName: String, amount: Int) {
    inventory.getOrPut(itemName) { ask("How many of $itemName is in your inventory? ").toInt() }
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
        is GatheredItem -> neededMaterials.reserveOf(amount, itemName)
        is CraftedItem -> handleCraftedItemShortage(amount, material)
    }
}

private fun handleCraftedItemShortage(amount: Int, item: CraftedItem) {
    val recipe = item.recipe()
    recipes += item
    val quantityProduced = recipe.quantityProduced
    if (quantityProduced != 1) {
        val minTimesToPerformRecipe = amount / quantityProduced
        if (minTimesToPerformRecipe != 0) recipe.ingredients.forEach {
            search(it.second, minTimesToPerformRecipe * it.first)
        }
        // TODO
        val overflow = amount % quantityProduced
        if (overflow != 0) neededMaterials.addToOverflowList(overflow, item)
        //if overflow != 0, add overflow item to overflow list
        recipe.ingredients.forEach { search(it.second, amount * it.first) }
    }
}

private fun allowUserToSearch(knownItems: List<Item>) {
    var lastSelection = listOf<Item>()
    while (true) {
        println("Please give a term to search (or the index of an item to create); ? to get BIS for a class [?CNJ25] # to exit:")
        val soughtMaterial = readln()
        if (soughtMaterial == "#") saveAndExit()
        if (soughtMaterial.startsWith("?")) findBestInSlot(soughtMaterial.drop(1))

        if (soughtMaterial.isNotBlank() && soughtMaterial.all { it.isDigit() }) { // select item by index
            processNumber(soughtMaterial, lastSelection)
        } else {
            lastSelection = knownItems.filter { it.name.contains(soughtMaterial) }
            lastSelection.forEachIndexed { index, material -> println("${index + 1}. $material") }
        }
    }
}

fun findBestInSlot(classAndLevel: String) {
    if (classAndLevel.all { it.isLowerCase() }) getBestConsumable(classAndLevel)
    else getBestGear(classAndLevel)
}

private fun getClassAndLevel(classAndLevel: String): Pair<Job?, Int?> {
    val (characterClass, levelAsString) = classAndLevel.span { it.isUpperCase() }
    val chosenClass = Job.values().find { it.abbreviation == characterClass }
    val level = levelAsString.toIntOrNull()
    return chosenClass to level
}

private fun getBestGear(classAndLevel: String) {
    val (chosenClass, level) = getClassAndLevel(classAndLevel)
    if (chosenClass == null || level == null || level <= 0) {
        println("'$classAndLevel' is NOT a valid character class and level combination")
    } else {
        val suitableGear =
            items.filterIsInstance<Gear>().filter { it.isSuitableFor(chosenClass, level) }.groupBy { it.slot }
        val prioritizedGear = suitableGear.mapValues { (_, v) -> v.sortedByDescending { it.level }.take(3) }
        val sortedSlots: List<Slot> = prioritizedGear.keys.sortedByDescending { prioritizedGear[it]!![0].level }
        sortedSlots.forEach { println(prioritizedGear[it]!!.joinToString(", ")) }
    }
}

fun getBestConsumable(attributeName: String) {
    val chosenAttribute = Stat.values().find { it.abbreviation == attributeName }
    if (chosenAttribute == null) {
        println("'$attributeName' is not a valid stat!")
        return
    }

    val consumableList = items.filterIsInstance<Consumable>().filter { chosenAttribute in it.stats }
    println(consumableList.sortedByDescending { it.stats[chosenAttribute] }.take(5).joinToString(", "))
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
    println("${soughtMaterial.name}: ${soughtMaterial.source.describe()}")
    soughtMaterial.source.analyze(soughtMaterial)
}

private fun categorizeMaterials(lines: List<String>): List<Item> {
    var currentCategory: Category? = null
    val rawMaterials = mutableListOf<Item>()
    for (line in lines) {
        when (line[0]) {
            '#' -> currentCategory = categoryFrom(line.substring(1).trim())
            '-' -> continue
            else -> {
                rawMaterials += Item.parse(line, currentCategory!!)
            }
        }
    }
    return rawMaterials
}


