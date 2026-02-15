package org.powbot.om6.api

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.bank.Quantity
import kotlin.random.Random

/**
 * Common inventory and banking utility functions.
 */
object InventoryUtils {

    /**
     * Checks if the inventory contains any items from a given list.
     *
     * @param itemNames The names of items to check for
     * @return True if at least one item from the list is in the inventory
     */
    fun hasAnyItem(vararg itemNames: String): Boolean {
        return Inventory.stream().name(*itemNames).count() > 0
    }

    /**
     * Checks if the inventory contains only items from a given list.
     *
     * @param itemNames The names of items to check
     * @return True if all items in inventory are in the provided list
     */
    fun inventoryContainsOnly(itemNames: List<String>): Boolean {
        val totalInvCount = Inventory.stream().count()
        val itemsInListCount = Inventory.stream().name(*itemNames.toTypedArray()).count()
        return totalInvCount == itemsInListCount
    }

    /**
     * Deposits items to the bank by name.
     *
     * @param itemNames The names of items to deposit
     * @param depositTimeout Timeout per item in milliseconds (default: 1000)
     * @param depositAttempts Max attempts per item (default: 10)
     * @return True if all items were successfully deposited
     */
    fun depositItems(
        vararg itemNames: String,
        depositTimeout: Int = 1000,
        depositAttempts: Int = 10
    ): Boolean {
        var allDeposited = true

        for ((index, itemName) in itemNames.withIndex()) {
            if (hasAnyItem(itemName)) {
                if (Bank.deposit().item(itemName, Quantity.of(Bank.Amount.ALL.value)).submit()) {
                    val isLastItem = index == itemNames.size - 1

                    if (isLastItem) {
                        val deposited = Condition.wait(
                            { !hasAnyItem(itemName) },
                            depositTimeout,
                            depositAttempts
                        )
                        if (!deposited) {
                            allDeposited = false
                        }
                    } else {
                        Condition.sleep(Random.nextInt(180, 400))
                    }
                } else {
                    allDeposited = false
                }
            }
        }
        return allDeposited
    }

    /**
     * Drops all items with specified names from the inventory.
     *
     * @param itemNames The names of items to drop
     * @param dropMinDelay Minimum delay between drops in milliseconds (default: 100)
     * @param dropMaxDelay Maximum delay between drops in milliseconds (default: 400)
     * @param dropTimeout Timeout for drop confirmation in milliseconds (default: 2000)
     * @param dropAttempts Max attempts for drop confirmation (default: 15)
     * @return True if the drop actions were initiated successfully
     */
    fun dropItems(
        vararg itemNames: String,
        dropMinDelay: Int = 80,
        dropMaxDelay: Int = 150,
        dropTimeout: Int = 50,
        dropAttempts: Int = 15
    ): Boolean {
        val itemsToDrop = Inventory.stream().name(*itemNames).list()
        if (itemsToDrop.isEmpty()) {
            return true
        }

        itemsToDrop.forEach { item ->
            if (item.interact("Drop")) {
                val delay = Random.nextInt(dropMinDelay, dropMaxDelay)
                Condition.sleep(delay)
            }
        }
        return Condition.wait(
            { !hasAnyItem(*itemNames) },
            dropTimeout,
            dropAttempts
        )
    }

    /**
     * Gets the inventory count.
     * @return The number of items in the inventory
     */
    fun getInventoryCount(): Int {
        return Inventory.stream().count().toInt()
    }

    /**
     * Gets the inventory count for a specific item name.
     *
     * @param itemName The name of the item to count
     * @return The number of items with that name in the inventory
     */
    fun count(itemName: String): Int {
        if (itemName.isBlank()) return 0
        return Inventory.stream().name(itemName).count().toInt()
    }

    /**
     * Ensures inventory tab is open.
     * @param sleepMin Minimum sleep time in milliseconds after opening (default: 100)
     * @param sleepMax Maximum sleep time in milliseconds after opening (default: 200)
     * @return true if inventory is open or was successfully opened
     */
    fun ensureInventoryOpen(sleepMin: Int = 100, sleepMax: Int = 200): Boolean {
        if (Inventory.opened()) return true
        if (Inventory.open()) {
            Condition.sleep(Random.nextInt(sleepMin, sleepMax))
            return true
        }
        return false
    }

    /**
     * Withdraws food from bank to fill inventory.
     * Useful for resuming tasks after food runs out.
     *
     * @param foodName The name of the food item to withdraw
     * @param targetLocation The tile to walk back to after withdrawing
     * @param withdrawTimeout Timeout per withdrawal in milliseconds (default: 2000)
     * @param withdrawAttempts Max withdrawal attempts (default: 10)
     * @return True if food was withdrawn and player walked back
     */
    fun withdrawFoodAndResume(
        foodName: String,
        targetLocation: Tile,
        withdrawTimeout: Int = 2000,
        withdrawAttempts: Int = 10
    ): Boolean {
        val currentCount = Inventory.stream().name(foodName).count()
        val emptySlots = 28 - getInventoryCount()

        if (emptySlots <= 0) {
            return false
        }

        // Withdraw as much as we can fit
        if (Bank.withdraw().item(foodName, Quantity.of(emptySlots.coerceAtMost(28))).submit()) {
            if (!Condition.wait({ Inventory.stream().name(foodName).count() > currentCount }, withdrawTimeout, withdrawAttempts)) {
                return false
            }
        } else {
            return false
        }

        // Walk back to task location
        Condition.sleep(Random.nextInt(300, 600))
        return Movement.walkTo(targetLocation)
    }
}
