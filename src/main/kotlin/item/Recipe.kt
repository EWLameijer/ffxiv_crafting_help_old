package item

import ask
import inventory
import items
import meaning
import unknowns
import Category.*

class Recipe(val quantityProduced: Int, val ingredients: List<Pair<Int, String>>) {
    companion object {
        class UserInputParser {
            private var amount = 1
            private val nameParts = mutableListOf<String>()
            val ingredients = mutableListOf<Pair<Int, String>>()

            fun handleComponent(component: String) = when (hasType(component)) {
                ComponentType.Number -> amount = component.toInt()
                ComponentType.Word -> handleWord(component)
                ComponentType.Abbreviated -> handleAbbreviation(component)
            }

            private fun handleAbbreviation(component: String) {
                amount = if (component[0].isDigit()) component.takeWhile { it.isDigit() }.toInt() else 1
                nameParts.clear()
                nameParts += component.dropWhile { it.isDigit() }
                val abbreviation = nameParts[0]
                while (abbreviation !in meaning.keys) {
                    val officialIngredientName = ask("Please give the full name of $abbreviation")
                    if (officialIngredientName in items.map { it.name }) meaning[abbreviation] =
                        officialIngredientName
                }
                ingredients += amount to meaning[abbreviation]!!
                resetParserForNextIngredient()
            }

            private fun handleWord(component: String) {
                nameParts += component
                val currentName = nameParts.joinToString(" ")
                val possibleItem = items.find { it.name == currentName }
                if (possibleItem != null) {
                    ingredients += amount to currentName
                    resetParserForNextIngredient()
                } // else: go to next cycle to read new (needed) term
            }

            private fun resetParserForNextIngredient() {
                nameParts.clear()
                amount = 1
            }

            fun handlePossibleRemainingIngredient() {
                if (nameParts.size != 0) {
                    val name = nameParts.joinToString(" ")
                    val answer = ask("I don't know $name. Put it on the unknown list?").lowercase()
                    if (answer[0] == 'y') {
                        unknowns += name
                        ingredients.add(amount to name)
                    }
                }
            }
        }

        fun obtainFromUser(name: String): Recipe {
            val quantityProduced = ask("How many items are produced by the $name recipe?").toInt()
            val ingredientCodes = ask("Please list the ingredients: ")
            // example "el ii ms" (or "2el ii ms" or 2 sunrise tellin bat fang)
            val components = ingredientCodes.split(' ')
            val parser = UserInputParser()
            for (component in components) parser.handleComponent(component)
            parser.handlePossibleRemainingIngredient()
            return Recipe(quantityProduced, parser.ingredients)
        }

        enum class ComponentType { Abbreviated, Number, Word }

        private fun hasType(component: String): ComponentType =
            if (component[0].isDigit()) {
                if (component.last().isDigit()) ComponentType.Number
                else ComponentType.Abbreviated
            } else if (component.length == 2) ComponentType.Abbreviated
            else ComponentType.Word

        class FromRecipeFileParser(private val inputParts: List<String>) {

            fun parse(): List<Pair<Int, String>> {
                var currentIndex = 0
                val ingredients = mutableListOf<Pair<Int, String>>()
                while (currentIndex + 1 <= inputParts.lastIndex) {
                    val amount = inputParts[currentIndex].takeWhile { it.isDigit() }.toInt()
                    currentIndex++
                    val pair = getItemName(currentIndex)
                    val itemName = pair.first
                    currentIndex = pair.second
                    ingredients.add(amount to itemName)
                }
                return ingredients
            }

            private fun getItemName(currentIndex: Int): Pair<String, Int> {
                var currentIndex1 = currentIndex
                var currentNamePart: String?
                val allNameParts = mutableListOf<String>()
                do {
                    currentNamePart = inputParts[currentIndex1]
                    val withoutTrailingComma =
                        if (currentNamePart.last() == ',') currentNamePart.dropLast(1) else currentNamePart
                    allNameParts.add(withoutTrailingComma)
                    currentIndex1++
                } while (currentIndex1 <= inputParts.lastIndex && currentNamePart!!.last() != ',')
                val itemName = allNameParts.joinToString(" ")
                return Pair(itemName, currentIndex1)
            }
        }

        // 1 produced by 1x iron ingot, 1x yew lumber, 1x clove oil
        fun parse(input: String): Recipe {
            val inputParts = input.split(' ')
            val quantityProduced = inputParts[0].toInt() // 1
            val parser = FromRecipeFileParser(inputParts.drop(3))
            val ingredients = parser.parse()
            return Recipe(quantityProduced, ingredients)
        }
    }

    override fun toString() =
        "$quantityProduced produced by ${ingredients.joinToString { "${it.first}x ${it.second}" }}"

    fun checkMaterialAvailability() {
        // TODO
        println("Checking ingredients!")
        ingredients.forEach { (neededAmount, itemName) ->
            if (inventory.containsKey(itemName) && inventory[itemName]!! >= neededAmount) println("I have enough $itemName!")
            else {
                val feedback = when (items.find { it.name == itemName }) {
                    null -> "Vendor item"
                    is CraftedItem -> "Crafted item"
                    else -> "Gathered item"
                }
                println(feedback)
            }

        }
    }
}