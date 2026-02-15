package org.powbot.community.herbrun.tasks

import org.powbot.community.herbrun.HerbRun

/**
 * Handles between-run cooldown pickpocketing at Farming Guild.
 */
class PickpocketTask(script: HerbRun) : HerbRunTask(script, "Between-Run Pickpocket") {
    override fun shouldExecute(): Boolean {
        return script.shouldPickpocketBetweenRuns()
    }

    override fun execute() {
        script.performBetweenRunPickpocketing()
    }
}

