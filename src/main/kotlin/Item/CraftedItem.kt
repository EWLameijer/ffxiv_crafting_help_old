package Item

import addRecipeToRecipes
import wishList

class CraftedItem(
    name: String,
    level: Int,
    category: Category.CraftingCategory,
    private val usage: Usage,
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

    override fun toString() = "$name ($category $level) [$usage] $recipe"

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

        /*
        A=hands / 	All (BLM/WHM/PAL)
Body	  	Black (shield: THM+PAL)
Consumable (no slot!)			Crafting
			Dragoon (mail)
Earrings
Feet
			Gathering
Head 		Healing (PIE-gear)
Ingedient (no slot)
Legs		Leather
Main hand 	Magic (DPS gear: int, no PIE)
Neck
Offhand
			Plate
Ring
            Support (craft AND gather)
Twohand
Wrists		White (CNJ/Pal shield)
         */

        // NOTE: For convenience, weapons are generally classed as their slot: main hand. Of course, there are
        // dual-wielders, like rogues, but generally each class equips one weapon. Exceptions: THM/BLM, CNJ/WHM:
        // can either have one-handed weapon or two-handed; T is reserved for them.
        enum class Slot(val abbreviation: Char) {
            Hands('A'), Body('B'), Earrings('E'), Feet('F'),
            Head('H'), Legs('L'), MainHand('M'), Neck('N'),
            OffHand('O'), Ring('R'), TwoHand('T'), Wrists('W')
        }

        // S/White/Black are for shields (all, CNJ/GLA, THM/GLA)
        enum class ClassRestriction(val abbreviation: String) {
            Black("B"), Leather("L"), Mail("M"), None("N"),
            Plate("P"), ShieldCapable("S"), White("W")
        }

        sealed class Usage {
            companion object {
                fun parse(input: String): Usage =
                    when (input) {
                        "I" -> Ingredient
                        "C" -> Consumable
                        else -> Gear.parse(input)
                    }

            }
        }
    }

    enum class Stat(val abbreviation: String) {
        // primary stats, one letter
        Dexterity("d"), Intelligence("i"), Mind("m"), Strength("s"),
        Vitality("v"),

        // secondary stats, two letters
        AttackMagicPotency("am"), AttackPower("ap"), CriticalHit("ch"),
        Control("co"), Craftmanship("cr"), Determination("de"),
        DirectHit("dh"), Defense("df"), Gathering("ga"),
        HealingMagicPotency("hm"), MagicDefense("md"), Perception("pe"),
        Piety("pi"), SkillSpeed("sk"), SpellSpeed("sp"), Tenacity("te")
    }


    class Gear(
        private val slot: Slot, private val level: Int, private val classRestriction: ClassRestriction,
        private val stats: Map<Stat, Int>
    ) : Usage() {
        companion object {
            private val slotAbbreviations = Slot.values().map { it.abbreviation }
            private val classRestrictionAbbreviations = ClassRestriction.values().map { it.abbreviation }
            private val statAbbreviations = Stat.values().map { it.abbreviation }


            // M34ARM
            fun parse(input: String): Gear {
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

            private fun parseItemStats(itemRestrictionAndStats: String): Pair<ClassRestriction, Map<Stat, Int>> {
                require(canParseItemStats(itemRestrictionAndStats)) { "parseItemStats: input was not properly validated!" }
                if (itemRestrictionAndStats.isEmpty()) return ClassRestriction.None to mapOf() // cosmetic gear
                val (rawRestriction, rawStats) = itemRestrictionAndStats.span { it.isUpperCase() }
                val classRestriction = if (rawRestriction == "") ClassRestriction.None else ClassRestriction.values()
                    .find { it.abbreviation == rawRestriction }!!
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

            private fun statsParser(statsString: String) = sequence<Pair<Stat, Int>> {
                var remainingStats = statsString
                while (remainingStats.isNotEmpty()) {
                    val (statName, statValueAndRest) = remainingStats.span { it.isLetter() }
                    val stat = Stat.values().find { it.abbreviation == statName}!!
                    val (valueAsString, rest) = statValueAndRest.span {it.isDigit()}
                    val value = valueAsString.toInt()
                    yield(stat to value)
                    remainingStats = rest
                }
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
                if (armorAsString.toInt() < 1) return false
                return (classRestriction.isEmpty() || classRestriction in classRestrictionAbbreviations)
                        && canParseStats(stats)
            }

            private fun canParseStats(stats: String): Boolean {
                if (stats.isEmpty()) return true
                if (!stats[0].isLowerCase()) return false
                val (statName, restOfStat) = stats.span { it.isLowerCase() }
                if (statName !in statAbbreviations) return false
                if (restOfStat.isEmpty() || !restOfStat[0].isDigit()) return false
                val (statSizeAsString, otherStats) = restOfStat.span { it.isDigit() }
                val statSize = statSizeAsString.toInt()
                if (statSize < 1) return false
                return canParseStats(otherStats)
            }
        }

        override fun toString(): String = "${slot.abbreviation}$level${classRestriction.abbreviation}"
    }

    object Ingredient : Usage() {
        override fun toString(): String = "I"
    }

    object Consumable : Usage() {
        override fun toString(): String = "C"
    }
}

fun String.span(predicate: (Char) -> Boolean): Pair<String, String> {
    val first = takeWhile(predicate)
    val second = dropWhile(predicate)
    return first to second
}
