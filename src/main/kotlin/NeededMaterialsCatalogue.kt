import Item.CraftedItem

class NeededMaterialsCatalogue {


    fun neededSoFar(itemName: String): Int {
        if (itemName !in basicMaterials.keys) basicMaterials[itemName] = 0
        return basicMaterials[itemName]!!
    }

    fun reserveOf(amount: Int, itemName: String) {
        basicMaterials[itemName] = basicMaterials[itemName]!! + amount
    }

    fun addToOverflowList(overflow: Int, material: CraftedItem) {
        
    }

    private val overflowList = mutableMapOf<String, Int>()

    private val basicMaterials = mutableMapOf<String, Int>()
}