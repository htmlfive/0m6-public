package org.powbot.om6.mortmyre.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Equipment
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.bank.Quantity
import org.powbot.om6.api.ScriptLogging
import org.powbot.om6.mortmyre.MortMyreFungusHarvester
import org.powbot.om6.mortmyre.Task
import org.powbot.om6.mortmyre.config.Constants

class BankAndGearTask(script: MortMyreFungusHarvester, name: String) : Task(script, name) {

    override fun activate(): Boolean {
        if (!script.isNearPoolTile()) return false
        if (script.isPrayerDepleted()) return false
        return script.hasFungusInInventory() || script.needsRingAction() || !hasSickleInInventory()
    }

    override fun execute() {
        if (!openBank()) return

        logAction(
            "Banking intent: hasFungus=${script.hasFungusInInventory()}, hasSickle=${hasSickleInInventory()}, ringEquipped=${script.isRingEquipped()}"
        )
        depositFungus()
        ensureSickleInInventory()
        ensureRingEquipped()

        if (Bank.opened()) {
            logAction("Closing bank")
            Bank.close()
            Condition.wait({ !Bank.opened() }, 150, 15)
        }
    }

    private fun openBank(): Boolean {
        if (Bank.opened()) return true
        logAction("Opening bank")
        if (!Bank.open()) {
            logWarn("Failed to open bank")
            Condition.sleep(300)
            return false
        }
        Condition.wait({ Bank.opened() }, 150, 20)
        if (!Bank.opened()) {
            logWarn("Bank did not open in time")
        }
        return Bank.opened()
    }

    private fun depositFungus() {
        if (!script.hasFungusInInventory()) return
        val fungusCount = Inventory.stream().name(Constants.FUNGUS_NAME).count(true).toInt()
        logAction("Depositing ${Constants.FUNGUS_NAME} (count=$fungusCount)")
        Bank.deposit().item(Constants.FUNGUS_NAME, Quantity.of(Bank.Amount.ALL.value)).submit()
        Condition.wait({ Inventory.stream().name(Constants.FUNGUS_NAME).isEmpty() }, 150, 20)
        if (script.hasFungusInInventory()) {
            val remaining = Inventory.stream().name(Constants.FUNGUS_NAME).count(true).toInt()
            logWarn("Deposit incomplete for ${Constants.FUNGUS_NAME}, remaining=$remaining")
        }
    }

    private fun ensureSickleInInventory() {
        if (hasSickleInInventory()) return

        if (Inventory.isFull()) {
            logWarn("Inventory full while missing ${Constants.SICKLE_NAME}, cannot withdraw")
            return
        }

        val sickleInBank = Bank.stream().name(Constants.SICKLE_NAME).first()
        if (!sickleInBank.valid()) {
            logWarn("${Constants.SICKLE_NAME} missing from inventory and bank")
            return
        }

        logAction("Withdrawing ${Constants.SICKLE_NAME}")
        val withdrew = Bank.withdraw().item(Constants.SICKLE_NAME, Quantity.of(1)).submit()
        if (!withdrew) {
            logWarn("Withdraw action failed for ${Constants.SICKLE_NAME}")
            return
        }

        val received = Condition.wait({ hasSickleInInventory() }, 150, 12)
        if (received) {
            logAction("${Constants.SICKLE_NAME} withdrawn successfully")
        } else {
            logWarn("Did not detect ${Constants.SICKLE_NAME} in inventory after withdraw")
        }
    }

    private fun ensureRingEquipped() {
        if (script.isRingEquipped()) return

        val preferredInventoryRing = Inventory.stream().name(Constants.PREFERRED_RING_NAME).first()
        val inventoryRing = if (preferredInventoryRing.valid()) {
            preferredInventoryRing
        } else {
            Inventory.stream().nameContains(Constants.RING_NAME_CONTAINS).first()
        }
        if (inventoryRing.valid()) {
            logAction("Equipping ring from inventory: ${inventoryRing.name()}")
            if (inventoryRing.interact(Constants.WEAR_ACTION) ||
                inventoryRing.interact(Constants.EQUIP_ACTION) ||
                inventoryRing.interact(Constants.WIELD_ACTION)
            ) {
                Condition.wait({ script.isRingEquipped() }, 150, 12)
            }
            return
        }

        val preferredBankRing = Bank.stream().name(Constants.PREFERRED_RING_NAME).first()
        val bankRing = if (preferredBankRing.valid()) {
            preferredBankRing
        } else {
            Bank.stream().nameContains(Constants.RING_NAME_CONTAINS).first()
        }
        if (!bankRing.valid()) {
            logWarn("No Ring of dueling found in inventory or bank")
            return
        }

        logAction("Withdrawing ${bankRing.name()}")
        if (Bank.withdraw(bankRing.name(), 1)) {
            Condition.wait({ Inventory.stream().nameContains(Constants.RING_NAME_CONTAINS).isNotEmpty() }, 150, 12)
            ensureRingEquipped()
        } else {
            logWarn("Withdraw action failed for ${bankRing.name()}")
        }
    }

    private fun hasSickleInInventory(): Boolean {
        return Inventory.stream().name(Constants.SICKLE_NAME).isNotEmpty()
    }

    private fun logAction(message: String) {
        ScriptLogging.action(script.logger, message)
    }

    private fun logWarn(message: String) {
        ScriptLogging.warn(script.logger, message)
    }
}
