package org.powbot.community.communitypowersalvage.tasks.execution

import org.powbot.api.Condition
import org.powbot.api.Random
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

        ScriptLogging.info(script.logger, "DROP: Dropping ${items.size} salvage items...")
        val dropped = items.size
        Inventory.drop(items)
        Condition.sleep(Random.nextInt(200, 400))
        ScriptLogging.info(script.logger, "DROP: Dropped $dropped items.")
    }
}
