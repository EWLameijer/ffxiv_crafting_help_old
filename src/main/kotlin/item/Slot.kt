package item
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
// Tights = Feet+Leg == Stockings (S)
enum class Slot(val abbreviation: Char) {
    Hands('A'), Body('B'), Cowl('C'), Earrings('E'), Feet('F'),
    Head('H'), Legs('L'), MainHand('M'), Neck('N'),
    OffHand('O'), Ring('R'), Stockings('S'), TwoHand('T'), Wrists('W');
}

val primarySlots = setOf(Slot.MainHand, Slot.TwoHand, Slot.OffHand)

