package org.powbot.community.herbrun.tasks

import org.powbot.community.herbrun.HerbRun

/**
 * Handles raking when a patch needs weeding.
 */
class WeedPatchTask(script: HerbRun) : HerbRunTask(script, "Weed Patch") {
    override fun shouldExecute(): Boolean {
        return script.shouldWeedPatch()
    }

    override fun execute() {
        script.weedCurrentPatch()
    }
}

