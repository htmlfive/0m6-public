package org.powbot.community.herbrun.tasks

import org.powbot.community.herbrun.HerbRun

/**
 * Handles harvesting ready herb patches.
 */
class HarvestPatchTask(script: HerbRun) : HerbRunTask(script, "Harvest Patch") {
    override fun shouldExecute(): Boolean {
        return script.shouldHarvestPatch()
    }

    override fun execute() {
        script.harvestCurrentPatch()
    }
}

