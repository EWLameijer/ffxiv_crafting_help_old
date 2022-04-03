import Category.*

sealed interface Category {

    enum class GatheringCategory(val abbreviation: String) : Category {
        Harvesting("HAR"), Logging("LOG"), Mining("MIN"), Quarrying("QUA"),
    }

    enum class CraftingCategory(val abbreviation: String) : Category {
        Alchemist("ALC"), Armorer("ARM"), Blacksmith("BSM"), Carpenter("CRP"),
        Culinarian("CUL"), Goldsmith("GSM"), Leatherworker("LTW"), Weaver("WVR")
    }
}

fun categoryFrom(input: String): Category {
    CraftingCategory.values().forEach {
        if (it.abbreviation == input) return it
    }
    GatheringCategory.values().forEach {
        if (it.abbreviation == input) return it
    }
    throw IllegalArgumentException("categoryFrom: cannot identify category '$input'.")
}
