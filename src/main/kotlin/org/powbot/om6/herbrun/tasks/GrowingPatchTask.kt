package org.powbot.om6.herbrun.tasks

import org.powbot.om6.herbrun.HerbRun

/**
 * Handles patches that are currently growing.
 */
class GrowingPatchTask(script: HerbRun) : HerbRunTask(script, "Patch Growing") {
    override fun shouldExecute(): Boolean {
        return script.shouldHandleGrowingPatch()
    }

    override fun execute() {
        script.handleGrowingPatchTask()
    }
}
