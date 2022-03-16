import java.io.File

enum class Category(val abbreviation: String) {
    Mining("MIN"), Quarrying("QUA"), Logging("LOG"),
    Harvesting("HAR")
}

fun categoryFrom(input: String): Category {
    Category.values().forEach {
        if (it.abbreviation == input) return it
    }
    throw IllegalArgumentException("categoryFrom: cannot identify category '$input'.")
}

class RawMaterial(val name: String, private val level: Int, private val category: Category) {
    override fun toString() = "$name ($category $level)"
}

fun parseItem(item: String): Pair<String, Int> {
    val level = item.takeWhile { it.isDigit() }.toInt()
    val name = item.dropWhile { !it.isLetter() }.trim()
    return name to level
}

fun main() {
    val lines = File("""D:\GoogleDriveEW\Hobby\Spellen\FFXIV\Lhei_Phoenix\crafting.txt""").readLines()
    val categoriesWithRawMaterials = lines.dropWhile { it != "#MIN" }.takeWhile { it != "$$" }

    val rawMaterials = categorizeRawMaterials(categoriesWithRawMaterials)
    rawMaterials.forEach(::println)
    while (true) {
        val soughtMaterial = readln()
        rawMaterials.filter { it.name.startsWith(soughtMaterial) }.forEach(::println)
    }
}

private fun categorizeRawMaterials(categoriesWithRawMaterials: List<String>): List<RawMaterial> {
    var currentCategory: Category? = null
    val rawMaterials = mutableListOf<RawMaterial>()
    for (item in categoriesWithRawMaterials) {
        when (item[0]) {
            '#' -> currentCategory = categoryFrom(item.substring(1).trim())
            '-' -> continue
            else -> {
                val (name, level) = parseItem(item)
                rawMaterials += RawMaterial(name, level, currentCategory!!)
            }
        }
    }
    return rawMaterials
}