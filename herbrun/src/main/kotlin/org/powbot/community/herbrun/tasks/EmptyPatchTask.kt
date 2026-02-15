package org.powbot.community.herbrun.tasks

import org.powbot.community.herbrun.HerbRun

/**
 * Handles composting and planting for empty patches.
 */
class EmptyPatchTask(script: HerbRun) : HerbRunTask(script, "Compost & Plant") {
    override fun shouldExecute(): Boolean {
        return script.shouldHandleEmptyPatch()
    }

    override fun execute() {
        script.handleEmptyPatchTask()
    }
}

