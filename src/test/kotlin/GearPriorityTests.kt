import item.Gear
import item.Slot
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

// see also https://github.com/junit-team/junit5-samples/blob/r5.8.2/junit5-jupiter-starter-gradle-kotlin/src/test/kotlin/com/example/project/CalculatorTests.kt

class GearPriorityTests {

    @ParameterizedTest
    @MethodSource("gearPriority")
    fun testPrioritizing(job: Job, level: Int, slot: Slot, prioritizedItemName: String) {
        val items = loadItems()
        val gearManager = GearManager(items)
        val actualName = gearManager.getPrioritizedGear(job, level)[slot]!![0].name
        assert(actualName == prioritizedItemName) {
            "$job$level $slot should lead to $prioritizedItemName, not $actualName."
        }
    }

    companion object {
        @JvmStatic
        private fun gearPriority() = listOf(
            Arguments.of(Job.Gladiator, 8, Slot.OffHand, "bronze hoplon"),
            Arguments.of(Job.Gladiator, 33, Slot.Body, "steel scale mail"),
            Arguments.of(Job.Gladiator, 34, Slot.OffHand, "bull hoplon")
        )
    }
}