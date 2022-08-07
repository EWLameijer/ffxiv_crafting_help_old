import java.io.File
import item.*
import item.Slot.*
import kotlin.system.exitProcess
import Job.*
import JobRestriction.*
import item.Stat.*
import item.Stat
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

object PersonalSettings {
    private const val fileName = "personal_settings.txt"

    fun charCode() = fetchWithKey("character_code")
    fun storyFile() = fetchWithKey("story_file")

    private fun fetchWithKey(key: String) = File(fileName).readLines().find { it.startsWith(key) }!!.split(" ")[1]
}

val armorSlots = setOf(Head, Body, Hands, Legs, Feet, Cowl, Stockings)

val jobLevels = File("levels.txt").readLines().map(::jobToLevel)

fun loadItems() = File("""D:\GoogleDriveEW\Hobby\Spellen\FFXIV\Lhei_Phoenix\crafting.txt""").readLines()
    .dropWhile { it != "#CRP" }.takeWhile { it != "$$" }.categorizeMaterials()

val itemFiles = listOf("wishlist.txt" to false, "have.txt" to true)

private fun jobToLevel(jobLevelAbbreviation: String): Pair<Job, Int> {
    val (jobStr, levelStr) = jobLevelAbbreviation.span { it.isUpperCase() }
    val job = Job.values().find { it.abbreviation == jobStr }
    val level = levelStr.toIntOrNull()
    require(job != null && level != null && level in 1..90) { "'$jobLevelAbbreviation' is illegal!" }
    return job to level
}

fun fetchLevelDataFromWebsite(): String {
    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create("https://eu.finalfantasyxiv.com/lodestone/character/${PersonalSettings.charCode()}/class_job/"))
        .build()

    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}

data class JobData(val level: Int = -1, val currentExp: Int = -1, val maxExp: Int = -1, val name: String = "unknown") {
    val preciseLevel: Double get() = level + currentExp.toDouble() / maxExp

    override fun toString(): String {
        val preciseLevelAsString = "%.3f".format(preciseLevel)
        return "$name: $preciseLevelAsString [$currentExp/$maxExp]"
    }
}

fun getClasses(): List<JobData> =
    fetchLevelDataFromWebsite().getAllListElements().filter { it.contains("character__job__exp") }
        .map { it.toJobData() }
        .filter { it.level > 0 }

fun String.getAllListElements(): List<String> {
    val result = mutableListOf<String>()
    var currentIndex = 0
    do {
        val nextListItemStart = indexOf("<li>", currentIndex)
        if (nextListItemStart == -1) break
        val nextListItemEnd = indexOf("</li>", nextListItemStart)
        result += substring(nextListItemStart..nextListItemEnd + 4)
        currentIndex = nextListItemEnd
    } while (true)
    return result
}

fun String.getHtmlValueByClass(className: String): String {
    // extremely simple. Should use some real parser... But that would be separate study project
    val location = indexOf(className)
    val endOfInitTag = indexOf(">", location)
    val startOfEndTag = indexOf("<", endOfInitTag)
    return substring(endOfInitTag + 1 until startOfEndTag)
}

fun String.toIntOrZero(): Int {
    val numberFormat = NumberFormat.getNumberInstance(Locale.ENGLISH) as DecimalFormat
    val trimmed = this.trim()
    return if (trimmed == "-") 0 else numberFormat.parse(trimmed).toInt()
}

fun String.toJobData(): JobData {
    val currentAndMaxExp = getHtmlValueByClass("character__job__exp").split("/")
    val currentExp = currentAndMaxExp[0].toIntOrZero()
    val maxExp = currentAndMaxExp[1].toIntOrZero()
    val level = getHtmlValueByClass("character__job__level").toIntOrZero()
    val name = getHtmlValueByClass("character__job__name")
    return JobData(level = level, currentExp = currentExp, maxExp = maxExp, name = name)
}

fun main() {
    showClassesAndLevels()

    val items = loadItems()
    val knownGear: MutableMap<Gear, Boolean> = loadKnownGear(items)
    val gearManager = GearManager(items, knownGear)
    checkRecipeLevelsUpToDate(items)

    gearManager.checkUsefulGear(jobLevels)

    knownGear.forEach { (item, have) -> println("$item => $have") }
    itemFiles.forEach { saveList(it.first, it.second, knownGear) }

    allowUserToSearch(gearManager, items)
}

private fun showClassesAndLevels() {
    val jobs = getClasses()
    jobs.forEach(::println)
    val textLength = File(PersonalSettings.storyFile()).readLines().indexOf("$$")
    println()
    with(jobs.map { it.preciseLevel }) {
        println("lines $textLength/min %.3f".format(minOrNull()) + "/avg %.3f".format(average()))
    }
    readln()
}

fun loadKnownGear(items: List<Item>) =
    itemFiles.flatMap { loadRelevantGear(it.first, it.second, items) }.toMap().toMutableMap()

fun checkRecipeLevelsUpToDate(items: List<Item>) {
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

private fun loadRelevantGear(fileName: String, haveItem: Boolean, items: List<Item>) =
    File(fileName).readLines().map { line -> line.dropWhile { it != ' ' }.drop(1) }
        .map { itemName -> items.find { it.name == itemName }!! as Gear to haveItem }

private fun saveList(fileName: String, haveItem: Boolean, knownGear: MutableMap<Gear, Boolean>) {
    val itemsToSave =
        knownGear.filterValues { it == haveItem }.keys.map { "${it.recommendedJobsType}${it.level} ${it.name}" }
            .sorted()
            .joinToString("\n")
    File(fileName).writeText(itemsToSave)
}

private fun allowUserToSearch(gearManager: GearManager, items: List<Item>) {
    while (true) {
        println("JOBlvl to get BIS for a class [CNJ25] # to exit:")
        val soughtMaterial = readln()
        if (soughtMaterial == "#") exitProcess(0)
        findBestInSlot(soughtMaterial, gearManager, items)
    }
}

fun findBestInSlot(classAndLevel: String, gearManager: GearManager, items: List<Item>) {
    if (classAndLevel.all { it.isLowerCase() }) getBestConsumable(classAndLevel, items)
    else getBestGear(classAndLevel, gearManager)
}

private fun getBestGear(jobAndLevel: String, gearManager: GearManager) {
    val (chosenClass, level) = jobToLevel(jobAndLevel)
    val prioritizedGear = gearManager.getPrioritizedGear(chosenClass, level)
    val sortedSlots: List<Slot> = prioritizedGear.keys.sortedByDescending { prioritizedGear[it]!![0].level }
    sortedSlots.forEach { println(prioritizedGear[it]!!.joinToString(", ")) }
}

fun getBestConsumable(attributeName: String, items: List<Item>) {
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
    sanityCheckOverall(rawMaterials)
    return rawMaterials
}

fun sanityCheck(newItem: Item) {
    if (newItem is Gear) {
        val gearStats = newItem.stats.keys
        val slot = newItem.slot
        val jobRestriction = newItem.jobRestriction
        val dowRestriction = setOf(Plate, Mail, Leather)
        if (jobRestriction in dowRestriction && (slot !in armorSlots ||
                    (Intelligence in gearStats || Mind in gearStats) || Defense !in gearStats)
        )
            throw Exception("Item $newItem has unexpected (incorrect?) stats.")
        if ((slot in setOf(MainHand, TwoHand)) && gearStats.isNotEmpty()) {
            throw Exception("Item $newItem has unexpected (incorrect?) stats.")
        }
        if (isShield(newItem) && newItem.stats[Defense] == null) {
            throw Exception("Item $newItem has unexpected (incorrect?) stats.")
        }
    }
}

// an offhand that can (also) be carried by a gladiator is a shield, as gladiators can carry all shields
private fun isShield(newItem: Gear) = newItem.slot == OffHand && Job.Gladiator in newItem.jobRestriction.jobs

fun sanityCheckOverall(currentItems: List<Item>) {
    currentItems.filterIsInstance<Gear>().filter(::isRegularDoWArmor).groupBy { it.level }.values.forEach { gear ->
        println(gear)
        val (bodyLegs, other) = gear.partition { it.slot in setOf(Body, Legs) }
        if (differentStatsUnder50(bodyLegs) || differentStatsUnder50(other))
            throw Exception("Item $bodyLegs or $other has unexpected (incorrect?) stats.")
    }
}

fun differentStatsUnder50(gear: List<Gear>) = gear.size > 1 && gear[0].level < 50 && gear.numDefenseValues() > 1

fun List<Gear>.numDefenseValues() = map { it.stats[Defense] }.distinct().size

private fun isRegularDoWArmor(it: Gear) =
    // 1: is this (regular) DoW armor?
    it.slot in armorSlots && it.slot !in setOf(Cowl, Stockings) && it.jobRestriction != None &&
            (it.jobRestriction.jobs.toList()[0].jobType !is GathererJob) && (it.jobRestriction.jobs.toList()[0].jobType !is CrafterJob)
            // silver tricorne: GREEN! Vintage gear also has superior armor
            && !it.name.startsWithGreenPrefix() && it.name !in knownGreenArmor

fun String.startsWithGreenPrefix() = prefixesOfGreenArmor.any { this.startsWith(it) }

val prefixesOfGreenArmor = setOf("vintage", "militia")
val knownGreenArmor = setOf("silver tricorne", "mosshorn scale mail")



