package org.powbot.community.herbrun.tasks

import org.powbot.community.herbrun.HerbRun

/**
 * Handles clearing dead herb patches.
 */
class DeadPatchTask(script: HerbRun) : HerbRunTask(script, "Clear Dead Patch") {
    override fun shouldExecute(): Boolean {
        return script.shouldClearDeadPatch()
    }

    override fun execute() {
        script.clearDeadPatchTask()
    }
}

