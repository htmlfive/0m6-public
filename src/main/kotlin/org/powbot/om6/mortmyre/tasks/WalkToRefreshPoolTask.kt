package org.powbot.om6.mortmyre.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Movement
import org.powbot.om6.mortmyre.MortMyreFungusHarvester
import org.powbot.om6.mortmyre.Task
import org.powbot.om6.mortmyre.config.Constants

class WalkToRefreshPoolTask(script: MortMyreFungusHarvester, name: String) : Task(script, name) {

    override fun activate(): Boolean {
        return script.shouldReturnToPool() && !script.hasNearbyPickableFungus() && !script.isNearPoolTile()
    }

    override fun execute() {
        if (Movement.walkTo(Constants.POOL_TILE)) {
            Condition.wait({ script.isNearPoolTile() }, 300, 12)
        } else {
            Condition.sleep(350)
        }
    }
}
