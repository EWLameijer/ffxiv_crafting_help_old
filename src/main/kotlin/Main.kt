import java.io.File
import item.*
import item.Slot.*
import kotlin.system.exitProcess

val armorSlots = setOf(Head, Body, Hands, Legs, Feet, Cowl, Stockings)

val items = mutableListOf<Item>()

fun main() {
    val lines = File("""D:\GoogleDriveEW\Hobby\Spellen\FFXIV\Lhei_Phoenix\crafting.txt""").readLines()
    val categoriesWithRawMaterials = lines.dropWhile { it != "#CRP" }.takeWhile { it != "$$" }

    items += categorizeMaterials(categoriesWithRawMaterials)
    items.forEach(::println)

    allowUserToSearch(items)
}


private fun allowUserToSearch(knownItems: List<Item>) {
    var lastSelection = listOf<Item>()
    while (true) {
        println("Please give a term to search (or the index of an item to create); ? to get BIS for a class [?CNJ25] # to exit:")
        val soughtMaterial = readln()
        if (soughtMaterial == "#") exitProcess(0)
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

        val prioritizedGear =
            suitableGear.mapValues { (_, v) -> v.sortedByDescending { it.scoreFor(chosenClass) }.take(3) }
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

private fun analyzeItem(soughtMaterial: Item) = println("${soughtMaterial.name}: ${soughtMaterial.source.describe()}")


private fun categorizeMaterials(lines: List<String>): List<Item> {
    var currentCategory: Category? = null
    val rawMaterials = mutableListOf<Item>()
    for (line in lines) {
        when (line[0]) {
            '#' -> currentCategory = categoryFrom(line.substring(1).trim())
            '-' -> continue
            else -> {
                val newItem = Item.parse(line, currentCategory!!)
                println(newItem)
                sanityCheck(newItem)
                rawMaterials += newItem
            }
        }
    }
    return rawMaterials
}

fun sanityCheck(newItem: Item) {
    if (newItem is Gear) {
        val gearStats = newItem.stats.keys
        val dowRestriction = setOf(JobRestriction.Plate, JobRestriction.Mail, JobRestriction.Leather)
        if (newItem.jobRestriction in dowRestriction && (newItem.slot !in armorSlots ||
                    (Stat.Intelligence in gearStats || Stat.Mind in gearStats) || Stat.Defense !in gearStats)
        )
            throw Exception("Item $newItem has unexpected (incorrect?) stats.")
    }
}




