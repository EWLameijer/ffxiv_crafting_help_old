import item.Stat

enum class Job(val abbreviation: String) {
    Arcanist("ACN"), Alchemist("ALC"), Archer("ARC"), Armorer("ARM"),
    Astrologian("AST"), Blacksmith("BSM"), Botanist("BTN"), Conjurer("CNJ"),
    Carpenter("CRP"), Culinarian("CUL"), DarkKnight("DRK"), Fisher("FSH"),
    Gladiator("GLA"), Goldsmith("GSM"), Lancer("LNC"), Leatherworker("LTW"),
    Machinist("MCH"), Miner("MIN"), Marauder("MRD"), Pugilist("PGL"),
    Rogue("ROG"), Scholar("SCH"), Thaumaturge("THM"), Weaver("WVR")
}

// on basis of stats
enum class JobRecommendation(val jobs: Set<Job>, val usefulStats: Set<Stat>, val isPrimaryStat: Boolean) {
    Crafter(
        setOf(
            Job.Alchemist, Job.Armorer, Job.Blacksmith, Job.Carpenter, Job.Culinarian, Job.Goldsmith,
            Job.Leatherworker, Job.Weaver
        ), setOf(Stat.Control, Stat.CP, Stat.Craftmanship),
        true
    ),
    Gatherer(setOf(Job.Botanist, Job.Fisher, Job.Miner), setOf(Stat.Perception, Stat.GP, Stat.Gathering), true),
    Healer(setOf(Job.Scholar, Job.Conjurer, Job.Astrologian), setOf(Stat.Piety, Stat.Mind), true),
    Caster(setOf(Job.Arcanist, Job.Thaumaturge), setOf(Stat.Intelligence), true),
    Dexterous(setOf(Job.Archer, Job.Rogue), setOf(Stat.Dexterity), true),
    Strong(JobRestriction.Mail.jobs + setOf(Job.Pugilist), setOf(Stat.Strength), true),
    Tank(JobRestriction.Plate.jobs, setOf(Stat.Tenacity, Stat.Defense), true),
    Adventurer(Healer.jobs + Caster.jobs + Dexterous.jobs + Strong.jobs, setOf(Stat.Defense, Stat.Vitality), false),
    All(Crafter.jobs + Gatherer.jobs + Adventurer.jobs, setOf(), false)
}

enum class JobRestriction(val abbreviation: String, val jobs: Set<Job>) {
    Arcanist(Job.Arcanist),
    Alchemist(Job.Alchemist),
    Archer(Job.Archer),
    Armorer(Job.Armorer),
    Astrologian(Job.Astrologian),
    Blacksmith(Job.Blacksmith),
    Botanist(Job.Botanist),
    Conjurer(Job.Conjurer),
    Carpenter(Job.Carpenter),
    Culinarian(Job.Culinarian),
    DarkKnight(Job.DarkKnight),
    Fisher(Job.Fisher),
    Gladiator(Job.Gladiator),
    Goldsmith(Job.Goldsmith),
    Lancer(Job.Lancer),
    Leatherworker(Job.Leatherworker),
    Machinist(Job.Machinist),
    Marauder(Job.Marauder),
    Miner(Job.Miner),
    Pugilist(Job.Pugilist),
    Thaumaturge(Job.Thaumaturge),
    Rogue(Job.Rogue),
    Scholar(Job.Scholar),
    Weaver(Job.Weaver),

    Black("B", setOf(Job.Gladiator, Job.Thaumaturge)),
    White("W", setOf(Job.Gladiator, Job.Conjurer)),
    ShieldCapable("S", setOf(Job.Gladiator, Job.Conjurer, Job.Thaumaturge)),
    Plate("P", setOf(Job.Gladiator, Job.Marauder, Job.DarkKnight)),
    Mail("M", Plate.jobs + setOf(Job.Lancer)),
    Leather("L", Mail.jobs + setOf(Job.Archer, Job.Machinist, Job.Rogue)),
    None("N", JobRecommendation.All.jobs);

    constructor (job: Job) : this(job.abbreviation, setOf(job))
}

