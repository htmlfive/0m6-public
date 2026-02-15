package org.powbot.community.mortmyre.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Objects
import org.powbot.community.api.feroxpool.FeroxPoolWaits
import org.powbot.community.mortmyre.MortMyreFungusHarvester
import org.powbot.community.mortmyre.Task
import org.powbot.community.mortmyre.config.Constants

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

