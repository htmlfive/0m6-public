package org.powbot.om6.herbrun.tasks

import org.powbot.om6.herbrun.HerbRun

/**
 * Handles the post-run Big Compost Bin collection/refill phase.
 */
class BigCompostBinTask(script: HerbRun) : HerbRunTask(script, "Big Compost Bin") {
    override fun shouldExecute(): Boolean {
        return script.shouldHandleBigCompostBin()
    }

    override fun execute() {
        script.handleBigCompostBinPhase()
    }
}
