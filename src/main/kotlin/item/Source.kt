package item

import Job
import Job.*

class Source(val level: Int, job: Job) {
    val manner: Job = if (job.jobType is CrafterJob) job
    else throw IllegalArgumentException("Source constructor exception: $job is not a crafter job!")

    override fun toString() = "$manner $level"
}
