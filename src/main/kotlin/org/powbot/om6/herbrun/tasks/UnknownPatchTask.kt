package org.powbot.om6.herbrun.tasks

import org.powbot.om6.herbrun.HerbRun

/**
 * Handles unknown patch states by waiting briefly.
 */
class UnknownPatchTask(script: HerbRun) : HerbRunTask(script, "Patch Unknown") {
    override fun shouldExecute(): Boolean {
        return script.shouldHandleUnknownPatch()
    }

    override fun execute() {
        script.handleUnknownPatchTask()
    }
}
