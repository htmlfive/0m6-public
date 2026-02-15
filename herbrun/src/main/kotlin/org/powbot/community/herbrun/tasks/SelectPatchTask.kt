package org.powbot.community.herbrun.tasks

import org.powbot.community.herbrun.HerbRun

/**
 * Pulls the next enabled patch from the queue and sets it as the active target.
 */
class SelectPatchTask(script: HerbRun) : HerbRunTask(script, "Select Patch") {
    override fun shouldExecute(): Boolean {
        return script.needsNextPatch()
    }

    override fun execute() {
        script.selectNextPatch()
    }
}

