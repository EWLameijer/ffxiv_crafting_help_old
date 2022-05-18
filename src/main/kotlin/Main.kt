import java.io.File
import item.*
import item.Slot.*
import kotlin.system.exitProcess

val armorSlots = setOf(Head, Body, Hands, Legs, Feet, Cowl, Stockings)

val items = mutableListOf<Item>()
val jobLevels = File("levels.txt").readLines().map(::jobToLevel)
val knownGear = mutableMapOf<Gear, Boolean>()

private fun jobToLevel(jobLevelAbbreviation: String): Pair<Job, Int> {
    val (jobStr, levelStr) = jobLevelAbbreviation.span { it.isUpperCase() }
    val job = Job.values().find { it.abbreviation == jobStr }
    val level = levelStr.toIntOrNull()
    require(job != null && level != null && level in 1..90) { "'$jobLevelAbbreviation' is illegal!" }
    return job to level
}

fun main() {
    val lines = File("""D:\GoogleDriveEW\Hobby\Spellen\FFXIV\Lhei_Phoenix\crafting.txt""").readLines()
    val categoriesWithRawMaterials = lines.dropWhile { it != "#CRP" }.takeWhile { it != "$$" }

    items += categorizeMaterials(categoriesWithRawMaterials)
    items.forEach(::println)

    knownGear += loadRelevantGear("wishlist.txt", false)
    knownGear += loadRelevantGear("have.txt", true)

    checkUsefulGear()

    knownGear.forEach { (item, have) -> println("$item => $have") }
    saveList("wishlist.txt", false)
    saveList("have.txt", true)
    allowUserToSearch()
}

private fun checkUsefulGear() {
    for ((job, currentLevel) in jobLevels) { // loop over jobs
        val prioritizedGear = getPrioritizedGear(job, currentLevel + 1)
        for (gearListForSlot in prioritizedGear.values) { // loop over gearslots for each job
            checkGearForSlot(gearListForSlot, currentLevel)
        }
    }
}

private fun checkGearForSlot(gearList: List<Gear>, currentLevel: Int) {
    val oneItemPerLevelList = gearList.distinctBy { it.level }
    val (futureGear, currentGear) = oneItemPerLevelList.span { it.level > currentLevel }

    if (futureGear.isNotEmpty()) registerItemPossession(futureGear[0])

    for (item in currentGear) {
        registerItemPossession(item)
        if (knownGear[item]!!) break
    }
}

private fun registerItemPossession(item: Gear) {
    if (item in knownGear.keys) return
    println(item)
    var answer = ""
    while (answer != "h" && answer != "w") {
        println("have(h) or wish(w)?")
        answer = readln()
    }
    knownGear[item] = (answer == "h")
}

private fun loadRelevantGear(fileName: String, haveItem: Boolean) =
    File(fileName).readLines().map { line -> line.dropWhile { it != ' ' }.drop(1) }
        .associate { itemName -> items.find { it.name == itemName }!! as Gear to haveItem }

private fun saveList(fileName: String, haveItem: Boolean) {
    val itemsToSave =
        knownGear.filterValues { it == haveItem }.keys.map { "${it.recommendedJobsType}${it.level} ${it.name}" }.sorted()
            .joinToString("\n")
    File(fileName).writeText(itemsToSave)
}

private fun allowUserToSearch() {
    while (true) {
        println("JOBlvl to get BIS for a class [CNJ25] # to exit:")
        val soughtMaterial = readln()
        if (soughtMaterial == "#") exitProcess(0)
        findBestInSlot(soughtMaterial)
    }
}

fun findBestInSlot(classAndLevel: String) {
    if (classAndLevel.all { it.isLowerCase() }) getBestConsumable(classAndLevel)
    else getBestGear(classAndLevel)
}

private fun getBestGear(jobAndLevel: String) {
    val (chosenClass, level) = jobToLevel(jobAndLevel)
    val prioritizedGear = getPrioritizedGear(chosenClass, level)
    val sortedSlots: List<Slot> = prioritizedGear.keys.sortedByDescending { prioritizedGear[it]!![0].level }
    sortedSlots.forEach { println(prioritizedGear[it]!!.joinToString(", ")) }
}

private fun getPrioritizedGear(
    chosenClass: Job,
    level: Int
): Map<Slot, List<Gear>> {
    val suitableGear =
        items.filterIsInstance<Gear>().filter { it.isSuitableFor(chosenClass, level) }.groupBy { it.slot }
    return suitableGear.mapValues { (_, v) -> v.sortedByDescending { it.scoreFor(chosenClass) }.take(3) }
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
        if ((newItem.slot == MainHand || newItem.slot == TwoHand) && newItem.jobRestriction.jobs.size != 1) {
            throw Exception("Item $newItem has unexpected (incorrect?) stats.")
        }
    }
}



