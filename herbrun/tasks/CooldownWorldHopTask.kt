package org.powbot.om6.herbrun.tasks

import org.powbot.om6.herbrun.HerbRun

/**
 * Handles cooldown-only world hopping checks/actions.
 */
class CooldownWorldHopTask(script: HerbRun) : HerbRunTask(script, "Cooldown World Hop") {
    override fun shouldExecute(): Boolean {
        return script.shouldHandleCooldownWorldHopTask()
    }

    override fun execute() {
        script.handleCooldownWorldHopTask()
    }
}
