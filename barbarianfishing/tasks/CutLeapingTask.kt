package org.powbot.om6.barbarianfishing.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Inventory
import org.powbot.om6.barbarianfishing.BarbarianFishing
import org.powbot.om6.barbarianfishing.Task
import kotlin.random.Random

class CutLeapingTask(script: BarbarianFishing, name: String) : Task(script, name) {

    override fun activate(): Boolean {
        return Inventory.stream().nameContains("Leaping").isNotEmpty() &&
            System.currentTimeMillis() >= script.nextCutAt
    }

    override fun execute() {
        if (!Inventory.opened()) {
            Inventory.open()
            Condition.wait({ Inventory.opened() }, 100, 10)
        }

        val knife = Inventory.stream().name("Knife").firstOrNull()
        val leapingFish = Inventory.stream().nameContains("Leaping").firstOrNull()
        if (knife == null || !knife.valid() || leapingFish == null || !leapingFish.valid()) {
            return
        }

        val selected = Inventory.selectedItem()
        if (selected.valid() && selected.id() != knife.id()) {
            selected.click()
            Condition.wait({ !Inventory.selectedItem().valid() }, 60, 6)
        }

        if (!knife.click()) {
            Condition.sleep(Random.nextInt(80, 151))
            return
        }

        Condition.sleep(Random.nextInt(80, 121))
        if (!Condition.wait({ Inventory.selectedItem().id() == knife.id() }, 80, 10)) {
            return
        }

        if (!leapingFish.click()) {
            Condition.sleep(Random.nextInt(80, 151))
            return
        }
        script.nextCutAt = System.currentTimeMillis() + 1800L

        if (!Condition.wait({ !Inventory.selectedItem().valid() }, 80, 12)) {
            return
        }

        script.fishNow(waitForAnimation = false)
    }
}
