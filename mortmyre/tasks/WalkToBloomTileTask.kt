package org.powbot.om6.mortmyre.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Players
import org.powbot.om6.mortmyre.MortMyreFungusHarvester
import org.powbot.om6.mortmyre.Task
import org.powbot.om6.mortmyre.config.Constants

class WalkToBloomTileTask(script: MortMyreFungusHarvester, name: String) : Task(script, name) {

    override fun activate(): Boolean {
        val nearbyFungi = Objects.stream()
            .name(Constants.BLOOM_OBJECT_NAME)
            .action(Constants.PICK_ACTION)
            .within(Players.local().tile(), 5.0)
            .nearest()
            .first()
        return !script.shouldReturnToPool() && !script.isAtBloomTile() && !nearbyFungi.valid()
    }

    override fun execute() {
        if (Movement.walkTo(Constants.BLOOM_TILE)) {
            Condition.wait({ script.isAtBloomTile() }, 300, 12)
        } else {
            Condition.sleep(350)
        }
    }
}
