package org.powbot.community.maplefletcher.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.community.maplefletcher.MapleFletcherConstants

class ShouldBank {
    fun validate(): Boolean {
        val hasLogs = Inventory.stream().name(MapleFletcherConstants.MAPLE_LOG_NAME).isNotEmpty()
        val hasBankableItems = Inventory.stream()
            .filtered {
                val name = it.name()
                name != MapleFletcherConstants.KNIFE_NAME &&
                    !name.contains("axe", ignoreCase = true)
            }
            .isNotEmpty()
        return !hasLogs && hasBankableItems
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

