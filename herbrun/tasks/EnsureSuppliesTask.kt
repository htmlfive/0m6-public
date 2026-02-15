package org.powbot.om6.herbrun.tasks

import org.powbot.om6.herbrun.HerbRun

/**
 * Validates that the player still has seeds + compost before starting a patch.
 */
class EnsureSuppliesTask(script: HerbRun) : HerbRunTask(script, "Validate Supplies") {
    override fun shouldExecute(): Boolean {
        return script.shouldCheckSupplies() && !script.hasRequiredSupplies()
    }

    override fun execute() {
        script.handleMissingSupplies()
    }
}
