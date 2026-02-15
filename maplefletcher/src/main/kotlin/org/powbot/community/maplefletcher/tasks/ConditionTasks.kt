package org.powbot.community.maplefletcher.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.community.maplefletcher.MapleFletcherConstants

class ShouldBank {
    fun validate(): Boolean {
        val hasBows = Inventory.stream().name(MapleFletcherConstants.MAPLE_LONGBOW_NAME).isNotEmpty()
        val hasLogs = Inventory.stream().name(MapleFletcherConstants.MAPLE_LOG_NAME).isNotEmpty()
        return hasBows || (Inventory.isFull() && !hasLogs)
    }
}

class ShouldFletch {
    fun validate(): Boolean {
        val hasLogs = Inventory.stream().name(MapleFletcherConstants.MAPLE_LOG_NAME).isNotEmpty()
        return Inventory.isFull() && hasLogs
    }
}

class NeedsKnife {
    fun validate(): Boolean = Inventory.stream().name(MapleFletcherConstants.KNIFE_NAME).isEmpty()
}

