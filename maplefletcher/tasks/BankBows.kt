package org.powbot.om6.maplefletcher.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Movement
import org.powbot.om6.maplefletcher.MapleFletcherConstants
import org.powbot.om6.mixology.structure.ScriptRecord
import org.powbot.om6.mixology.structure.TreeTask
import java.util.logging.Logger

class BankBows(private val record: ScriptRecord) : TreeTask(true) {
    private val logger = Logger.getLogger(BankBows::class.java.name)

    override fun execute(): Int {
        if (!walkToBank()) return super.execute()
        if (!Bank.opened()) {
            if (!Bank.open()) {
                logger.warning("Unable to open bank.")
                return super.execute()
            }
            Condition.wait({ Bank.opened() }, 200, 10)
        }
        val bowsInInventory = Inventory.stream().name(MapleFletcherConstants.MAPLE_LONGBOW_NAME)
        val bowCount = bowsInInventory.count(true).toInt()
        if (bowCount > 0) {
            if (Bank.depositAllExcept(MapleFletcherConstants.KNIFE_NAME)) {
                Condition.wait(
                    { Inventory.stream().name(MapleFletcherConstants.MAPLE_LONGBOW_NAME).isEmpty() },
                    200,
                    10
                )
                val total = record.getNotedValue("bows_banked")
                record.setNotedValue("bows_banked", total + bowCount)
            } else {
                logger.warning("Failed to deposit Maple longbow (u).")
            }
        }
        if (Inventory.stream().name(MapleFletcherConstants.KNIFE_NAME).isEmpty()) {
            if (Bank.withdraw(MapleFletcherConstants.KNIFE_NAME, 1)) {
                Condition.wait(
                    { Inventory.stream().name(MapleFletcherConstants.KNIFE_NAME).isNotEmpty() },
                    200,
                    10
                )
            } else {
                logger.warning("Knife not found in bank when banking bows.")
            }
        }
        Bank.close()
        return super.execute()
    }

    private fun walkToBank(): Boolean {
        val bankTile = record.getNotedPosition("maple_bank_tile") ?: MapleFletcherConstants.BANK_TILE
        if (bankTile.distance() <= MapleFletcherConstants.BANK_RADIUS) return true
        if (Movement.step(bankTile)) {
            Condition.wait(
                { bankTile.distance() <= MapleFletcherConstants.BANK_RADIUS },
                250,
                16
            )
        }
        return bankTile.distance() <= MapleFletcherConstants.BANK_RADIUS
    }

    override fun toString(): String = "Banking Maple longbows"
}
