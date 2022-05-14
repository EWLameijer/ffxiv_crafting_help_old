package item

import Category
import Category.*
import Job
import JobRestriction
import item.Stat.*
import span

abstract class Item(val name: String, open val source: Source) {
    override fun toString() = "$name ($source)"

    companion object {
        fun parse(input: String, currentCategory: Category) = when (currentCategory) {
            is GatheringCategory -> parseGatheredMaterial(input, currentCategory)
            is CraftingCategory -> parseFromMasterFile(input, currentCategory)
        }

        // 34 @steel doming hammer M34ARM
        private fun parseFromMasterFile(input: String, category: CraftingCategory): Item {
            val components = input.trim().split(" ")
            val level = components[0].toInt()
            val furtherComponents = components.drop(1)
            val (nameWithAt, usage) = furtherComponents.dropLast(1).joinToString(" ").trim() to
                    furtherComponents.last().trim()
            val name = nameWithAt.drop(1)
            val source = Crafting(level, category)
            return itemWithProperUsage(usage, name, source)
        }

        private fun parseGatheredMaterial(input: String, category: GatheringCategory): GatheredIngredient {
            val dataWithPossibleLocation = input.split(":")
            val components = dataWithPossibleLocation[0].trim().split(" ")
            val level = components[0].toInt()
            val furtherComponents = components.drop(1)
            val nameWithAt = furtherComponents.joinToString(" ").trim()

            val source = Gathering(level, category)

            return GatheredIngredient(nameWithAt.drop(1), source)
        }

        private fun itemWithProperUsage(usage: String, name: String, source: Crafting) = when {
            usage == "I" -> CraftedIngredient(name, source)
            Consumable.canParse(usage) -> Consumable.parse(name, source, usage)
            else -> Gear.parse(name, source, usage)
        }
    }
}

abstract class CraftedItem(name: String, override val source: Crafting) : Item(name, source)

abstract class GatheredItem(name: String, override val source: Gathering) : Item(name, source)

class CraftedIngredient(name: String, source: Crafting) : CraftedItem(name, source)

class GatheredIngredient(name: String, source: Gathering) : GatheredItem(name, source)

abstract class StatsProvidingItem(name: String, source: Crafting, val stats: Map<Stat, Int>) :
    CraftedItem(name, source) {
    protected fun statsToString() = stats.toList().joinToString(", ") { (stat, size) -> "$stat: $size" }

    companion object {

        private val statAbbreviations = Stat.values().map { it.abbreviation }

        fun canParseStats(statistics: String): Boolean {
            if (statistics.isEmpty()) return true
            if (!statistics[0].isLowerCase()) return false
            val (statName, restOfStat) = statistics.span { it.isLowerCase() }
            if (statName !in statAbbreviations) return false
            if (restOfStat.isEmpty() || !restOfStat[0].isDigit()) return false
            val (statSizeAsString, otherStats) = restOfStat.span { it.isDigit() }
            val statSize = statSizeAsString.toInt()
            if (statSize < 1) return false
            return canParseStats(otherStats)
        }

        fun statsParser(statsString: String) = sequence {
            var remainingStats = statsString
            while (remainingStats.isNotEmpty()) {
                val (statName, statValueAndRest) = remainingStats.span { it.isLetter() }
                val stat = Stat.values().find { it.abbreviation == statName }!!
                val (valueAsString, rest) = statValueAndRest.span { it.isDigit() }
                val value = valueAsString.toInt()
                yield(stat to value)
                remainingStats = rest
            }
        }
    }
}

class Consumable(name: String, source: Crafting, stats: Map<Stat, Int>) : StatsProvidingItem(name, source, stats) {
    override fun toString() = "$name (${statsToString()})"

    companion object {
        fun canParse(input: String): Boolean {
            if (input == "C") return true
            if (input[1].isDigit()) return false // is a cowl
            return canParseStats(input.substring(1))
        }

        fun parse(name: String, source: Crafting, usageAsString: String): Consumable {
            val stats = statsParser(usageAsString.drop(1)).toMap()
            return Consumable(name, source, stats)
        }
    }
}

class GearScore(
    private val statsScore: Int,
    private val vitality: Int,
    private val defense: Int,
    private val level: Int,
    private val restriction: Int
) : Comparable<GearScore> {
    override operator fun compareTo(other: GearScore): Int = when {
        statsScore != other.statsScore -> statsScore - other.statsScore
        defense != other.defense -> defense - other.defense
        vitality != other.vitality -> vitality - other.vitality
        level != other.level -> level - other.level
        restriction != other.restriction -> restriction - other.restriction
        else -> 0
    }
}

class Gear(
    name: String, val slot: Slot, val level: Int, val jobRestriction: JobRestriction,
    source: Crafting, stats: Map<Stat, Int>
) : StatsProvidingItem(name, source, stats) {

    override fun toString(): String {
        val statsString = if (stats.isEmpty()) "" else ", ${statsToString()}"
        return "$name (Level $level$statsString)"
    }

    fun isSuitableFor(job: Job, jobLevel: Int): Boolean =
        level <= jobLevel && job in getPermittedJobs() && job in getRecommendedJobs()

    private fun getRecommendedJobs(): Set<Job> =
        JobRecommendation.values().filter { jobType -> jobType.usefulStats.any { it in stats.keys } }
            .flatMap { it.jobs }.toSet() + if (jobRestriction.jobs.size == 1) jobRestriction.jobs else setOf()

    private fun getPermittedJobs(): Set<Job> = jobRestriction.jobs

    private fun getStat(stat: Stat) = stats[stat] ?: 0

    // note: need to sort on gear stats, not level, else
    // goatskin leggings (Level 17, Defense: 33, MagicDefense: 33, Strength: 2, Dexterity: 2, Vitality: 2, SkillSpeed: 2)
    // may wind up before
    // cotton trousers (Level 17, Defense: 44, MagicDefense: 44, Strength: 3, Dexterity: 3, Vitality: 3, SkillSpeed: 3)
    fun scoreFor(chosenClass: Job): GearScore {
        val correctCategories = JobRecommendation.values().filter { chosenClass in it.jobs && it.isPrimaryStat }
        val statsScore = correctCategories.flatMap { it.usefulStats }.sumOf(::getStat)
        return GearScore(statsScore, getStat(Vitality), getStat(Defense), level, restrictionScore())
    }

    private fun restrictionScore() = -jobRestriction.jobs.size // the more restricted, the better

    companion object {
        private val slotAbbreviations = Slot.values().map { it.abbreviation }
        private val jobRestrictionAbbreviations = JobRestriction.values().map { it.abbreviation }

        // M5ARCd1
        fun parse(name: String, source: Crafting, usage: String): Gear {
            require(
                usage.length >= 2 && usage[0] in slotAbbreviations && usage[1].isDigit() &&
                        canParseLastPart(usage)
            ) {
                println("Incorrect usage $usage!")
            }
            val slot = Slot.values().find { it.abbreviation == usage[0] }!!
            val level = usage.dropWhile { it.isLetter() }.takeWhile { it.isDigit() }.toInt()
            val (classRestriction, stats) = parseItemStats(
                usage.substring(1).dropWhile { it.isDigit() })

            return Gear(name, slot, level, classRestriction, source, stats)
        }

        private fun canParseLastPart(input: String): Boolean {
            // first checks succeeded, so first is letter, then number.
            val fromLevel = input.drop(1)
            val afterLevel = fromLevel.dropWhile { it.isDigit() }
            // expect now either cr15co10 (if unrestricted) or L65s2 (Disciple of War, strength2)
            return canParseItemStats(afterLevel)
        }

        private fun canParseItemStats(afterLevel: String): Boolean {
            if (afterLevel.isEmpty()) return true // "F1" means something cosmetic
            val (classRestriction, rawStats) = afterLevel.span { it.isUpperCase() }
            // 65s2 (from L65s2)
            val (armorAsString, stats) = rawStats.span { it.isDigit() }
            if (armorAsString.isNotBlank() && armorAsString.toInt() < 1) return false
            return (classRestriction.isEmpty() || classRestriction in jobRestrictionAbbreviations)
                    && canParseStats(stats)
        }

        private fun parseItemStats(itemRestrictionAndStats: String): Pair<JobRestriction, Map<Stat, Int>> {
            require(canParseItemStats(itemRestrictionAndStats)) { "parseItemStats: input was not properly validated!" }
            if (itemRestrictionAndStats.isEmpty()) return JobRestriction.None to mapOf() // cosmetic gear
            val (rawRestriction, rawStats) = itemRestrictionAndStats.span { it.isUpperCase() }
            val classRestriction = getJobRestriction(rawRestriction)
            if (rawStats.isEmpty()) return classRestriction to mapOf()
            val (armorString, rest) = rawStats.span { it.isDigit() }
            val currentStats = mutableMapOf<Stat, Int>()
            if (armorString != "") {
                val armor = armorString.toInt()
                currentStats[Defense] = armor
                currentStats[MagicDefense] = armor
            }
            statsParser(rest).forEach { currentStats[it.first] = it.second }
            return classRestriction to currentStats
        }

        private fun getJobRestriction(rawRestriction: String): JobRestriction {
            if (rawRestriction != "") JobRestriction.values().find { it.abbreviation == rawRestriction }
                ?: throw IllegalArgumentException("Cannot understand class restriction '$rawRestriction'")
            return if (rawRestriction == "") JobRestriction.None else JobRestriction.values()
                .find { it.abbreviation == rawRestriction }!!
        }
    }
}



