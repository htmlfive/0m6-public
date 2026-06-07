package org.powbot.community.herbrun.tasks

import org.powbot.community.herbrun.HerbRun

class CurePatchTask(script: HerbRun) : HerbRunTask(script, "Cure Diseased Patch") {
    override fun shouldExecute(): Boolean {
        return script.shouldHandleDiseasedPatch()
    }

    override fun execute() {
        script.handleDiseasedPatchTask()
    }
}
