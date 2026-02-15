package org.powbot.om6.herbrun.tasks

import org.powbot.om6.herbrun.HerbRun

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
