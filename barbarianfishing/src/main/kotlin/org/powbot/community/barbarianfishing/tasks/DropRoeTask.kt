package org.powbot.community.barbarianfishing.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Inventory
import org.powbot.community.barbarianfishing.BarbarianFishing
import org.powbot.community.barbarianfishing.Task
import kotlin.random.Random

class DropRoeTask(script: BarbarianFishing, name: String) : Task(script, name) {

    override fun activate(): Boolean {
        return Inventory.isFull() && Inventory.stream().name("Roe").isNotEmpty()
    }

    override fun execute() {
        if (!Inventory.opened()) {
            Inventory.open()
            Condition.wait({ Inventory.opened() }, 100, 10)
        }

        val roeItems = Inventory.stream().name("Roe").list()
        if (roeItems.isEmpty()) {
            return
        }

        for (roe in roeItems) {
            if (!roe.valid()) {
                continue
            }
            roe.interact("Drop")
            Condition.sleep(Random.nextInt(80, 151))
        }
    }
}

