import item.Stat
import item.Stat.*
import ArmorType.*
import Job.*

enum class ArmorType { Leather, Mail, Plate }

enum class Job(val abbreviation: String, val jobType: JobType, val descendants: Set<Job> = setOf()) {
    Alchemist("ALC", CrafterJob()), Archer("ARC", DexterityJob()),
    Armorer("ARM", CrafterJob()), Astrologian("AST", HealerJob()),
    Blacksmith("BSM", CrafterJob()), Botanist("BTN", GathererJob()),
    Carpenter("CRP", CrafterJob()), Culinarian("CUL", CrafterJob()),
    DarkKnight("DRK", TankJob()), Fisher("FSH", GathererJob()),
    Goldsmith("GSM", CrafterJob()), Lancer("LNC", MailJob()),
    Leatherworker("LTW", CrafterJob()), Machinist("MCH", DexterityJob()),
    Miner("MIN", GathererJob()), Marauder("MRD", TankJob()),
    Ninja("NIN", DexterityJob()), Paladin("PLD", TankJob()),
    Pugilist("PGL", StrengthLeatherJob()), Scholar("SCH", HealerJob()),
    Summoner("SMN", CasterJob()), Thaumaturge("THM", CasterJob()),
    Weaver("WVR", CrafterJob()), WhiteMage("WHM", HealerJob()),

    Arcanist("ACN", CasterJob(), setOf(Scholar, Summoner)),
    Conjurer("CNJ", HealerJob(), setOf(WhiteMage)),
    Gladiator("GLA", TankJob(), setOf(Paladin)),
    Rogue("ROG", DexterityJob(), setOf(Ninja));

    open class JobType(val mainStats: Set<Stat>, val supportingStats: Set<Stat> = setOf()) {
        fun relevantStats() = mainStats + supportingStats
    }
    class GathererJob : JobType(setOf(), setOf(Perception, GP, Gathering))
    class CrafterJob : JobType(setOf(), setOf(Control, CP, Craftmanship))
    abstract class AdventurerJob(mainStat: Stat, supportingStats: Set<Stat>) : JobType(setOf(Vitality, mainStat), supportingStats)

    private class HealerJob : AdventurerJob(Mind, setOf(Piety))
    private class CasterJob : AdventurerJob(Intelligence, setOf())
    abstract class WarJob(val maxArmor: ArmorType, mainStat: Stat, supportingStats: Set<Stat> = setOf()) : AdventurerJob(mainStat, supportingStats)

    private class DexterityJob : WarJob(Leather, Dexterity)
    abstract class StrengthJob(maxArmor: ArmorType, supportingStats: Set<Stat> = setOf()) :
        WarJob(maxArmor, Strength, supportingStats)

    private class StrengthLeatherJob : StrengthJob(Leather)
    class MailJob : StrengthJob(Mail)
    class TankJob : StrengthJob(Plate, setOf(Tenacity, Defense))
}

inline fun <reified T> getJobsOfType() = Job.values().filter { it.jobType is T }.toSet()

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
    Plate("P", getJobsOfType<TankJob>()),
    Mail("M", Plate.jobs + getJobsOfType<MailJob>()),
    Leather("L", getJobsOfType<WarJob>()),
    None("N", getJobsOfType<Any>());

    constructor (job: Job) : this(job.abbreviation, job.descendants + job)
}

