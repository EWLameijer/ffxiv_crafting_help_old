import java.io.File
import item.*
import item.Slot.*
import kotlin.system.exitProcess
import Job.*
import JobRestriction.*

val armorSlots = setOf(Head, Body, Hands, Legs, Feet, Cowl, Stockings)

val jobLevels = File("levels.txt").readLines().map(::jobToLevel)
val items = loadItems()

fun loadItems() = File("""D:\GoogleDriveEW\Hobby\Spellen\FFXIV\Lhei_Phoenix\crafting.txt""").readLines()
    .dropWhile { it != "#CRP" }.takeWhile { it != "$$" }.categorizeMaterials()

val itemFiles = listOf("wishlist.txt" to false, "have.txt" to true)
val knownGear: MutableMap<Gear, Boolean> =
    itemFiles.flatMap { loadRelevantGear(it.first, it.second) }.toMap().toMutableMap()
val gearManager = GearManager(items)

private fun jobToLevel(jobLevelAbbreviation: String): Pair<Job, Int> {
    val (jobStr, levelStr) = jobLevelAbbreviation.span { it.isUpperCase() }
    val job = Job.values().find { it.abbreviation == jobStr }
    val level = levelStr.toIntOrNull()
    require(job != null && level != null && level in 1..90) { "'$jobLevelAbbreviation' is illegal!" }
    return job to level
}

fun main() {
    checkRecipeLevelsUpToDate()

    gearManager.checkUsefulGear(jobLevels)

    knownGear.forEach { (item, have) -> println("$item => $have") }
    itemFiles.forEach { saveList(it.first, it.second) }

    allowUserToSearch()
}

fun checkRecipeLevelsUpToDate() {
    jobLevels.forEach { (job, level) ->
        if (job in getJobsOfType<CrafterJob>()) {
            val jobItems = items.filter { it.source.manner == job }
            for (checkLevel in -2..level step 5) {
                // ranges are 1..5, 6..10 etc, opened at range -3 (level 21 recipes at level 18).
                val startLevel = checkLevel + 3
                val endLevel = startLevel + 4
                val levelRange = startLevel..endLevel
                if (jobItems.none { it.source.level in levelRange })
                    throw Exception("Cannot find a ${job.abbreviation} recipe for levelrange $levelRange.")
            }
        }
    }
}


private fun loadRelevantGear(fileName: String, haveItem: Boolean) =
    File(fileName).readLines().map { line -> line.dropWhile { it != ' ' }.drop(1) }
        .map { itemName -> items.find { it.name == itemName }!! as Gear to haveItem }

private fun saveList(fileName: String, haveItem: Boolean) {
    val itemsToSave =
        knownGear.filterValues { it == haveItem }.keys.map { "${it.recommendedJobsType}${it.level} ${it.name}" }
            .sorted()
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
    val prioritizedGear = gearManager.getPrioritizedGear(chosenClass, level)
    val sortedSlots: List<Slot> = prioritizedGear.keys.sortedByDescending { prioritizedGear[it]!![0].level }
    sortedSlots.forEach { println(prioritizedGear[it]!!.joinToString(", ")) }
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

private fun List<String>.categorizeMaterials(): List<Item> {
    var currentJob: Job? = null
    val rawMaterials = mutableListOf<Item>()
    for (line in this) {
        when (line[0]) {
            '#' -> currentJob = Job.values().find { it.abbreviation == line.substring(1).trim() }
            '-' -> continue
            else -> {
                val newItem = Item.parseFromMasterFile(line, currentJob!!)
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
        val dowRestriction = setOf(Plate, Mail, Leather)
        if (newItem.jobRestriction in dowRestriction && (newItem.slot !in armorSlots ||
                    (Stat.Intelligence in gearStats || Stat.Mind in gearStats) || Stat.Defense !in gearStats)
        )
            throw Exception("Item $newItem has unexpected (incorrect?) stats.")
        if ((newItem.slot == MainHand || newItem.slot == TwoHand) && newItem.jobRestriction.jobs.size != 1) {
            throw Exception("Item $newItem has unexpected (incorrect?) stats.")
        }
        if (isShield(newItem) && newItem.stats[Stat.Defense] == null) {
            throw Exception("Item $newItem has unexpected (incorrect?) stats.")
        }
    }
}

// an offhand that can (also) be carried by a gladiator is a shield, as gladiators can carry all shields
private fun isShield(newItem: Gear) = newItem.slot == OffHand && Job.Gladiator in newItem.jobRestriction.jobs



