package item

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