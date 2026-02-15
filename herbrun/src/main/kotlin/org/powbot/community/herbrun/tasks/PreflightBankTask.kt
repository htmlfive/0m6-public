package org.powbot.community.herbrun.tasks

import org.powbot.community.herbrun.HerbRun

/**
 * Performs a one-time banking pass at the start of the run to ensure required tools and supplies.
 */
class PreflightBankTask(script: HerbRun) : HerbRunTask(script, "Preflight Bank") {
    override fun shouldExecute(): Boolean {
        return !script.preflightComplete
    }

    override fun execute() {
        script.performPreflightBanking()
    }
}

