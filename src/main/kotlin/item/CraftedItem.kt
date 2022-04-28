package item

import Job
import JobRecommendation
import JobRestriction
import addRecipeToRecipes
import span
import wishList

// TODO: Likely logic works better if Consumable and Gear would be subclasses of CraftedItem, NOT components.
class CraftedItem(
    name: String,
    level: Int,
    category: Category.CraftingCategory,
    val usage: Usage,
    var recipe: Recipe?
) :
    Item(name, category, level) {

    override fun analyze() {
        if (recipe != null) println(recipe)
        else {
            recipe = Recipe.obtainFromUser(name)
            addRecipeToRecipes(this)
        }
        println("Put on wish list?")
        val reply = readln()
        if (reply.uppercase().startsWith("Y")) {
            wishList += name
            recipe!!.checkMaterialAvailability()
        }
    }

    override fun toString() = "$name ($category $level) [$usage]" + (recipe ?: "")

    companion object {
        // example "initiate's awl (Blacksmith 23) [O23] 1 produced by 1x iron ingot, 1x yew lumber, 1x clove oil"
        fun parse(input: String): CraftedItem {
            val items = input.split(' ')
            val nameParts = items.takeWhile { it[0] != '(' } // initiate's awl
            val restStart = nameParts.size // 2
            val category = Category.CraftingCategory.valueOf(items[restStart].drop(1)) // Blacksmith
            val level = items[restStart + 1].dropLast(1).toInt() // 23
            val usage = Usage.parse(items[restStart + 2].drop(1).dropLast(1)) // M23GLA
            val recipeAsString = items.drop(restStart + 3).joinToString(" ").trim()
            val name = nameParts.joinToString(" ")
            return CraftedItem(name, level, category, usage, Recipe.parse(recipeAsString))
        }

        // 34 @steel doming hammer M34ARM
        fun parseFromSource(input: String, category: Category.CraftingCategory): CraftedItem {
            val components = input.trim().split(" ")
            val level = components[0].toInt()
            val furtherComponents = components.drop(1)
            val (nameWithAt, rawUsage) = if (furtherComponents.last().any { it.isUpperCase() })
                furtherComponents.dropLast(1).joinToString(" ").trim() to furtherComponents.last().trim()
            else furtherComponents.joinToString(" ").trim() to null
            val usage = Usage.parse(rawUsage!!)
            return CraftedItem(nameWithAt.drop(1), level, category, usage, null)
        }

        sealed class Usage {
            companion object {
                fun parse(input: String): Usage =
                    if (input == "I") Ingredient
                    else if (Consumable.canParse(input)) Consumable.parse(input)
                    else Gear.parse(input)
            }
        }

        open class UsageWithStats(val stats: Map<Stat, Int>) : Usage() {
            protected fun statsToString() = stats?.toList().joinToString(", ") { (stat, size) -> "$stat: $size" }

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

        enum class Stat(val abbreviation: String) {
            // primary stats, one letter
            Dexterity("d"), Intelligence("i"), Mind("m"), Strength("s"),
            Vitality("v"),

            // secondary stats, two letters
            AttackMagicPotency("am"), AttackPower("ap"), CP("cp"), CriticalHit("ch"),
            Control("co"), Craftmanship("cr"), Determination("de"),
            DirectHit("dh"), Defense("df"), Gathering("ga"), GP("gp"),
            HealingMagicPotency("hm"), HP("hp"), MagicDefense("md"), MP("mp"), Perception("pe"),
            Piety("pi"), SkillSpeed("sk"), SpellSpeed("sp"), Tenacity("te")
        }

        class Gear(
            val slot: Slot, val level: Int, val jobRestriction: JobRestriction,
            stats: Map<Stat, Int>
        ) : UsageWithStats(stats) {

            fun isSuitableFor(job: Job, jobLevel: Int): Boolean =
                level <= jobLevel && job in getPermittedJobs() && job in getRecommendedJobs()

            private fun getRecommendedJobs(): Set<Job> {
                val recommendedJobs = mutableSetOf<Job>()
                if (statsHaveAnyOf(Stat.Piety, Stat.Mind)) recommendedJobs += JobRecommendation.Healer.jobs
                if (Stat.Intelligence in stats.keys) recommendedJobs += JobRecommendation.Caster.jobs
                if (statsHaveAnyOf(
                        Stat.Control,
                        Stat.CP,
                        Stat.Craftmanship
                    )
                ) recommendedJobs += JobRecommendation.Crafter.jobs
                if (statsHaveAnyOf(
                        Stat.Perception,
                        Stat.GP,
                        Stat.Gathering
                    )
                ) recommendedJobs += JobRecommendation.Gatherer.jobs
                if (Stat.Strength in stats.keys) recommendedJobs += JobRecommendation.Strong.jobs
                if (Stat.Dexterity in stats.keys) recommendedJobs += JobRecommendation.Dexterous.jobs
                if (jobRestriction.jobs.size == 1) recommendedJobs += jobRestriction.jobs // sometimes I neglect stats on class-only gear
                if (jobRestriction in setOf(
                        JobRestriction.Black,
                        JobRestriction.White,
                        JobRestriction.ShieldCapable
                    )
                ) recommendedJobs += jobRestriction.jobs
                return recommendedJobs
            }

            private fun getPermittedJobs(): Set<Job> = jobRestriction.jobs

            private fun statsHaveAnyOf(vararg statsToTry: Stat): Boolean {
                for (stat in statsToTry) {
                    if (stat in stats) return true
                }
                return false
            }

            companion object {
                private val slotAbbreviations = Slot.values().map { it.abbreviation }
                private val jobRestrictionAbbreviations = JobRestriction.values().map { it.abbreviation }

                // M5ARCd1
                fun parse(input: String): Gear {
                    println("Parse with input '$input'")
                    require(
                        input.length >= 2 && input[0] in slotAbbreviations && input[1].isDigit() &&
                                canParseLastPart(input)
                    ) {
                        println("Incorrect usage $input!")
                    }
                    val slot = Slot.values().find { it.abbreviation == input[0] }!!
                    val level = input.dropWhile { it.isLetter() }.takeWhile { it.isDigit() }.toInt()
                    val (classRestriction, stats) = parseItemStats(input.substring(1).dropWhile { it.isDigit() })

                    return Gear(slot, level, classRestriction, stats)
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
                        currentStats[Stat.Defense] = armor
                        currentStats[Stat.MagicDefense] = armor
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
            }

            override fun toString(): String =
                "${slot.abbreviation}$level${jobRestriction.abbreviation}${statsToString()}"
        }

        object Ingredient : Usage() {
            override fun toString(): String = "I"
        }

        class Consumable(stats: Map<Stat, Int>) : UsageWithStats(stats) {
            companion object {
                fun canParse(input: String): Boolean {
                    if (input == "C") return true
                    if (input[1].isDigit()) return false // is a cowl
                    return canParseStats(input.substring(1))
                }

                fun parse(input: String): Consumable {
                    val stats = statsParser(input.drop(1)).toMap()
                    return Consumable(stats)
                }
            }

            override fun toString() = "C${statsToString()}"
        }
    }
}


