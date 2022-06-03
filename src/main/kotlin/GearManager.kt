import item.Gear
import item.Item
import item.Slot

class GearManager(items: List<Item>) {
    private val gear = items.filterIsInstance<Gear>()

    fun checkUsefulGear(jobLevels: List<Pair<Job, Int>>) {
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

    fun getPrioritizedGear(chosenClass: Job, level: Int): Map<Slot, List<Gear>> {
        val suitableGear = gear.filter { it.isSuitableFor(chosenClass, level) }.groupBy { it.slot }
        return suitableGear.mapValues { (_, v) -> v.sortedByDescending { it.scoreFor(chosenClass) }.take(3) }
    }
}