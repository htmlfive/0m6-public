package org.powbot.community.barbarianfishing.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Inventory
import org.powbot.community.barbarianfishing.BarbarianFishing
import org.powbot.community.barbarianfishing.Task
import kotlin.random.Random

class FishTask(script: BarbarianFishing, name: String) : Task(script, name) {

    override fun activate(): Boolean {
        return Inventory.stream().nameContains("Leaping").isEmpty()
    }

    override fun execute() {
        if (!script.fishNow()) {
            Condition.sleep(Random.nextInt(80, 151))
            return
        }

        Condition.sleep(200)
    }
}

