package org.powbot.community.mortmyre.tasks

import org.powbot.api.Condition
import org.powbot.community.api.ScriptLogging
import org.powbot.community.api.WorldHopUtils
import org.powbot.community.mortmyre.MortMyreFungusHarvester
import org.powbot.community.mortmyre.Task

class HopIfOccupiedTask(script: MortMyreFungusHarvester, name: String) : Task(script, name) {

    override fun activate(): Boolean {
        return !script.shouldReturnToPool() && script.hasPlayersNearBloomTile()
    }

    override fun execute() {
        val hopped = WorldHopUtils.hopToRandomWorld()
        if (!hopped) {
            ScriptLogging.warn(script.logger, "Failed to hop worlds while bloom tile area is occupied.")
            Condition.sleep(900)
            return
        }
        Condition.sleep(1200)
    }
}

