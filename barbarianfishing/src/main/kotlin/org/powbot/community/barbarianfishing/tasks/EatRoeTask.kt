package org.powbot.community.barbarianfishing.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Inventory
import org.powbot.community.barbarianfishing.BarbarianFishing
import org.powbot.community.barbarianfishing.Task
import kotlin.random.Random

class EatRoeTask(script: BarbarianFishing, name: String) : Task(script, name) {

    override fun activate(): Boolean {
        return Inventory.stream().name("Roe").isNotEmpty() &&
            System.currentTimeMillis() >= script.nextRoeEatAt &&
            System.currentTimeMillis() >= script.nextEatAt
    }

    override fun execute() {
        val roe = Inventory.stream().name("Roe").firstOrNull()
        if (roe == null || !roe.valid()) {
            return
        }

        if (Inventory.selectedItem().valid()) {
            if (!Condition.wait({ !Inventory.selectedItem().valid() }, 80, 12)) {
                return
            }
        }

        if (!roe.click()) {
            Condition.sleep(Random.nextInt(80, 151))
            return
        }

        Condition.sleep(Random.nextInt(80, 121))
        script.nextEatAt = System.currentTimeMillis() + 2000L
        script.nextRoeEatAt = 0L
        script.fishNow(waitForAnimation = false)
    }
}

