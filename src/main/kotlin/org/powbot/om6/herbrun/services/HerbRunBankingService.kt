package org.powbot.om6.herbrun.services

import org.powbot.api.Condition
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Inventory
import kotlin.random.Random

class HerbRunBankingService(
    private val defaultRetries: Int,
    private val logWarn: (String) -> Unit
) {
    fun openBankWithRetries(reason: String, attempts: Int = defaultRetries): Boolean {
        repeat(attempts) { attempt ->
            if (Bank.opened()) {
                return true
            }
            val opened = Bank.open() && Condition.wait({ Bank.opened() }, 120, 20)
            if (opened) {
                return true
            }
            logWarn("Bank open failed for $reason (attempt ${attempt + 1}/$attempts)")
            Condition.sleep(Random.nextInt(160, 280))
        }
        return Bank.opened()
    }

    fun depositInventoryWithRetries(attempts: Int = defaultRetries): Boolean {
        repeat(attempts) { attempt ->
            if (Inventory.stream().isEmpty()) {
                return true
            }
            val deposited = Bank.depositInventory() && Condition.wait({ Inventory.stream().isEmpty() }, 120, 20)
            if (deposited) {
                return true
            }
            logWarn("Bank deposit inventory failed (attempt ${attempt + 1}/$attempts)")
            Condition.sleep(Random.nextInt(140, 240))
        }
        return Inventory.stream().isEmpty()
    }

    fun withdrawByNameWithRetries(name: String, amount: Int, attempts: Int = defaultRetries): Boolean {
        repeat(attempts) { attempt ->
            val before = Inventory.stream().name(name).count(true).toInt()
            val withdrew = Bank.withdraw(name, amount)
            val reached = Condition.wait(
                { Inventory.stream().name(name).count(true).toInt() >= before + amount },
                120,
                20
            )
            if (withdrew && reached) {
                return true
            }
            logWarn("Bank withdraw failed for $name x$amount (attempt ${attempt + 1}/$attempts)")
            Condition.sleep(Random.nextInt(140, 240))
        }
        return false
    }

    fun withdrawByIdWithRetries(
        itemId: Int,
        inventoryCheck: () -> Int,
        targetIncrease: Int,
        attempts: Int = defaultRetries
    ): Boolean {
        repeat(attempts) { attempt ->
            val before = inventoryCheck()
            val withdrew = Bank.withdraw(itemId, targetIncrease)
            val reached = Condition.wait({ inventoryCheck() >= before + targetIncrease }, 120, 20)
            if (withdrew && reached) {
                return true
            }
            logWarn("Bank withdraw failed for itemId=$itemId amount=$targetIncrease (attempt ${attempt + 1}/$attempts)")
            Condition.sleep(Random.nextInt(140, 240))
        }
        return false
    }
}
