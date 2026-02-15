package org.powbot.community.pohcake.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Inventory
import org.powbot.community.pohcake.PohCakeMaker
import org.powbot.community.pohcake.Task

private const val ITEM_POT = "Pot"
private const val ITEM_CAKE = "Cake"
private const val ITEM_BUCKET = "Bucket"
private const val ITEM_BURNT_CAKE = "Burnt cake"

class DropItemsTask(script: PohCakeMaker) : Task(script, "Drop Items") {

    override fun activate(): Boolean =
        Inventory.emptySlotCount() < 4 &&
            script.hasDropItems()

    override fun execute() {
        script.ensureShiftDrop()
        val items = Inventory.stream()
            .name(ITEM_POT, ITEM_CAKE, ITEM_BUCKET, ITEM_BURNT_CAKE)
            .toList()

        if (items.isEmpty()) {
            return
        }

        script.logger.info("Dropping ${items.size} items.")
        Inventory.drop(items)
        Condition.wait(
            { Inventory.stream().name(ITEM_POT, ITEM_CAKE, ITEM_BUCKET, ITEM_BURNT_CAKE).isEmpty() },
            150,
            20
        )
    }
}

