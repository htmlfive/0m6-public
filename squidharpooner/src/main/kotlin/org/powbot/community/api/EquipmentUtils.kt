package org.powbot.community.api

import org.powbot.api.Condition
import org.powbot.api.rt4.Equipment
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Item

/**
 * Common equipment and item utility functions.
 */
object EquipmentUtils {

    /**
     * Equips an item from inventory to a specific slot.
     * @param item The item to equip
     * @param targetSlot The target equipment slot
     * @param timeout Timeout for equip in milliseconds (default: 2500)
     * @param attempts Max attempts for equip (default: 10)
     * @return true if item was successfully equipped
     */
    fun equipItem(
        item: Item,
        targetSlot: Equipment.Slot,
        timeout: Int = 2500,
        attempts: Int = 10
    ): Boolean {
        val actions = item.actions()
        val actionUsed = listOf("Wield", "Wear", "Equip").find { it in actions } ?: return false

        return if (item.interact(actionUsed)) {
            Condition.wait({ Equipment.itemAt(targetSlot).id() == item.id() }, timeout / attempts, attempts)
        } else {
            false
        }
    }

    /**
     * Checks if an item is equipped in a specific slot.
     * @param itemId The item ID to check
     * @param slot The equipment slot to check
     * @return true if the item is equipped in that slot
     */
    fun isItemEquipped(itemId: Int, slot: Equipment.Slot): Boolean {
        return Equipment.itemAt(slot).id() == itemId
    }

    /**
     * Checks if any of the given item IDs are equipped in a slot.
     * @param itemIds The item IDs to check
     * @param slot The equipment slot to check
     * @return true if any item is equipped in that slot
     */
    fun isAnyItemEquipped(itemIds: List<Int>, slot: Equipment.Slot): Boolean {
        val equippedId = Equipment.itemAt(slot).id()
        return equippedId in itemIds
    }

    /**
     * Gets the item name equipped in a slot.
     * @param slot The equipment slot
     * @return The item name, or empty string if no item equipped
     */
    fun getEquippedItemName(slot: Equipment.Slot): String {
        return Equipment.itemAt(slot).name()
    }

    /**
     * Finds the first item in inventory matching any of the given names.
     * @param itemNames The item names to search for
     * @return The first matching item, or Item.Nil if none found
     */
    fun findAnyItem(vararg itemNames: String): Item {
        return Inventory.stream().name(*itemNames).firstOrNull() ?: Item.Nil
    }

    /**
     * Checks if inventory contains all specified items.
     * @param itemNames The item names to check for
     * @return true if all items are in inventory
     */
    fun hasAllItems(vararg itemNames: String): Boolean {
        return itemNames.all { itemName ->
            Inventory.stream().name(itemName).isNotEmpty()
        }
    }
}

