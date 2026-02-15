package org.powbot.community.herblore.services

import org.powbot.api.Condition
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Item

class InventoryInteractionService {

    fun useItemOnItem(primary: Item, secondary: Item): Boolean {
        if (!primary.valid() || !secondary.valid()) return false
        if (primary.useOn(secondary)) return true

        if (!primary.interact("Use")) return false
        val selected = Condition.wait(
            { Inventory.selectedItem().valid() && Inventory.selectedItem().id() == primary.id() },
            80,
            12
        )
        if (!selected) return false

        return secondary.click()
    }
}

