package org.powbot.om6.herbrun.tasks

import org.powbot.om6.herbrun.HerbRun

/**
 * Handles the travel logic between patches (teleports, walking, etc).
 */
class TravelToPatchTask(script: HerbRun) : HerbRunTask(script, "Travel To Patch") {
    override fun shouldExecute(): Boolean {
        return script.shouldTravelToPatch()
    }

    override fun execute() {
        script.travelToActivePatch()
    }
}
