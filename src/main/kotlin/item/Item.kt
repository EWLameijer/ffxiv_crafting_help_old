package item

import Category

abstract class Item(val name: String, val category: Category, val level: Int) {
    abstract fun analyze()
}

