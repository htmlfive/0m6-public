package org.powbot.community.herblore.services

import org.powbot.api.Condition
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Widgets

class HerbloreBankingService(
    private val onAction: ((String) -> Unit)? = null,
    private val onWarn: ((String) -> Unit)? = null
) {
    private companion object {
        const val MAX_RETRIES = 3
    }

    fun ensureOpen(): Boolean {
        onAction?.invoke("Bank open intent")
        return retryBankAction {
            if (Bank.opened()) return@retryBankAction true
            if (!Bank.open()) return@retryBankAction false
            Condition.wait({ Bank.opened() }, 120, 25)
        }
    }

    fun ensureClosed(): Boolean {
        onAction?.invoke("Bank close intent")
        return retryBankAction {
            if (!Bank.opened()) return@retryBankAction true
            if (!Bank.close()) return@retryBankAction false
            Condition.wait({ !Bank.opened() }, 120, 15)
        }
    }

    fun depositInventory(): Boolean {
        onAction?.invoke("Deposit intent: inventory all")
        return retryBankAction {
            if (Inventory.stream().isEmpty()) return@retryBankAction true
            val depositAllButton = Widgets.component(12, 41)
            if (!depositAllButton.valid() || !depositAllButton.click()) return@retryBankAction false
            Condition.wait({ Inventory.stream().isEmpty() }, 120, 25)
        }
    }

    fun withdraw(name: String, amount: Int): Boolean {
        onAction?.invoke("Withdraw intent: $name x$amount")
        return retryBankAction {
            val before = Inventory.stream().name(name).count(true).toInt()
            if (!Bank.withdraw(name, amount)) return@retryBankAction false
            Condition.wait(
                { Inventory.stream().name(name).count(true).toInt() > before || Bank.stream().name(name).isEmpty() },
                120,
                25
            )
        }
    }

    fun hasInBank(name: String): Boolean {
        return Bank.stream().name(name).isNotEmpty()
    }

    private fun retryBankAction(action: () -> Boolean): Boolean {
        repeat(MAX_RETRIES) { attempt ->
            onAction?.invoke("Bank attempt ${attempt + 1}/$MAX_RETRIES")
            if (action()) return true
            onWarn?.invoke("Bank attempt ${attempt + 1}/$MAX_RETRIES failed")
            if (attempt < MAX_RETRIES - 1) {
                Condition.sleep(120)
            }
        }
        return false
    }
}

