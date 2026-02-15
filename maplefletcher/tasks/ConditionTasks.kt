package org.powbot.om6.maplefletcher.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.om6.maplefletcher.MapleFletcherConstants
import org.powbot.om6.mixology.structure.TreeTask

class ShouldBank : TreeTask(false) {
    override fun validate(): Boolean {
        val hasBows = Inventory.stream().name(MapleFletcherConstants.MAPLE_LONGBOW_NAME).isNotEmpty()
        val hasLogs = Inventory.stream().name(MapleFletcherConstants.MAPLE_LOG_NAME).isNotEmpty()
        return hasBows || (Inventory.isFull() && !hasLogs)
    }
}

class ShouldFletch : TreeTask(false) {
    override fun validate(): Boolean {
        val hasLogs = Inventory.stream().name(MapleFletcherConstants.MAPLE_LOG_NAME).isNotEmpty()
        return Inventory.isFull() && hasLogs
    }
}

class NeedsKnife : TreeTask(false) {
    override fun validate(): Boolean = Inventory.stream().name(MapleFletcherConstants.KNIFE_NAME).isEmpty()
}
