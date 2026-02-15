package org.powbot.community.herbrun.tasks

import org.powbot.community.herbrun.HerbRun

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

