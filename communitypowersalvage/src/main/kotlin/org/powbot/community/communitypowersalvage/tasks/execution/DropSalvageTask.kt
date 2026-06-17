package org.powbot.community.communitypowersalvage.tasks.execution

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.Game
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Players
import org.powbot.community.api.ScriptLogging
import org.powbot.community.communitypowersalvage.CommunityPowerSalvage
import org.powbot.community.communitypowersalvage.tasks.base.Task

class DropSalvageTask(script: CommunityPowerSalvage) : Task(script) {
    override fun activate(): Boolean {
        return Players.local().animation() == -1 && Inventory.stream().filtered { it.name().contains("salvage", ignoreCase = true) }.isNotEmpty()
    }

    override fun execute() {
        script.status = "Dropping"

        val items = Inventory.stream().filtered { it.name().contains("salvage", ignoreCase = true) }.toList()
        if (items.isEmpty()) {
            return
        }

        ScriptLogging.info(script.logger, "DROP: Dropping ${items.size} salvage items (tapToDrop=${script.tapToDrop})...")
        items.sortedBy { zigzagPriority(it.inventoryIndex()) }.forEach { item ->
            if (!item.valid()) return@forEach
            if (!Inventory.opened()) { Inventory.open() }
            if (script.tapToDrop) {
                item.click()
            } else {
                item.interact("Drop")
            }
            Condition.sleep(Random.nextInt(80, 151))
        }

        val dropped = Condition.wait({
            Inventory.stream().filtered { it.name().contains("salvage", ignoreCase = true) }.count() < items.size.toLong()
        }, Random.nextInt(90, 151), 30)

        if (!dropped) {
            script.stopScript("DROP: Count stuck at ${items.size} for 3s, stopping.")
            return
        }
        ScriptLogging.info(script.logger, "DROP: Dropped ${items.size} items.")
    }

    companion object {
        private fun zigzagPriority(slot: Int): Int {
            val col = slot % 4
            val row = slot / 4
            return if (col < 2) col * 7 + row else (col - 2) * 7 + row + 14
        }
    }
}
