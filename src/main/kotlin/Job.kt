import item.Stat
import item.Stat.*
import ArmorType.*
import Job.*

enum class ArmorType { Leather, Mail, Plate }

enum class Job(val abbreviation: String, val jobType: JobType) {
    Arcanist("ACN", CasterJob()), Alchemist("ALC", CrafterJob()),
    Archer("ARC", DexterityJob()), Armorer("ARM", CrafterJob()),
    Astrologian("AST", HealerJob()), Blacksmith("BSM", CrafterJob()),
    Botanist("BTN", GathererJob()), Conjurer("CNJ", HealerJob()),
    Carpenter("CRP", CrafterJob()), Culinarian("CUL", CrafterJob()),
    DarkKnight("DRK", TankJob()), Fisher("FSH", GathererJob()),
    Gladiator("GLA", TankJob()), Goldsmith("GSM", CrafterJob()),
    Lancer("LNC", MailJob()), Leatherworker("LTW", CrafterJob()),
    Machinist("MCH",DexterityJob()), Miner("MIN", GathererJob()),
    Marauder("MRD", TankJob()), Pugilist("PGL", StrengthLeatherJob()),
    Rogue("ROG",DexterityJob()), Scholar("SCH", HealerJob()),
    Thaumaturge("THM", CasterJob()), Weaver("WVR", CrafterJob());

    open class JobType(val primaryStats: Set<Stat>)
    class GathererJob : JobType(setOf(Perception, GP, Gathering))
    class CrafterJob : JobType(setOf(Control, CP, Craftmanship))
    abstract class AdventurerJob(primaryStats: Set<Stat>) : JobType(primaryStats)

    private abstract class MagicJob(primaryStats: Set<Stat>) : AdventurerJob(primaryStats)

    private class HealerJob : MagicJob(setOf(Piety, Mind))
    private class CasterJob : MagicJob(setOf(Intelligence))
    abstract class WarJob(val maxArmor: ArmorType, mainStats: Set<Stat>) : AdventurerJob(mainStats)

    private class DexterityJob : WarJob(Leather, setOf(Dexterity))
    abstract class StrengthJob(maxArmor: ArmorType, otherStats: Set<Stat> = setOf()) :
        WarJob(maxArmor, otherStats + Strength)

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

    constructor (job: Job) : this(job.abbreviation, setOf(job))
}

