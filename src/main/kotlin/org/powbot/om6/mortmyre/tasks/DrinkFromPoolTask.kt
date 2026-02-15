package org.powbot.om6.mortmyre.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Objects
import org.powbot.om6.api.feroxpool.FeroxPoolWaits
import org.powbot.om6.mortmyre.MortMyreFungusHarvester
import org.powbot.om6.mortmyre.Task
import org.powbot.om6.mortmyre.config.Constants

class DrinkFromPoolTask(script: MortMyreFungusHarvester, name: String) : Task(script, name) {

    override fun activate(): Boolean {
        return script.shouldReturnToPool() && script.isNearPoolTile() && script.isPrayerDepleted()
    }

    override fun execute() {
        val pool = Objects.stream().name(Constants.POOL_NAME).action(Constants.DRINK_ACTION).nearest().first()
        if (!pool.valid()) {
            Condition.sleep(250)
            return
        }

        if (pool.interact(Constants.DRINK_ACTION)) {
            // Requested sequence: wait for animation start, then animation end, then fixed settle sleep.
            FeroxPoolWaits.waitForDrinkCycleAndSettle()
        }
    }
}
