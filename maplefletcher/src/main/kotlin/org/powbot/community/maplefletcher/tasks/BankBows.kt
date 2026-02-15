package org.powbot.community.maplefletcher.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Movement
import org.powbot.community.maplefletcher.MapleFletcher
import org.powbot.community.maplefletcher.MapleFletcherConstants
import java.util.logging.Logger

class BankBows(private val script: MapleFletcher) : Task {
    private val logger = Logger.getLogger(BankBows::class.java.name)

    override fun execute(): Int {
        if (!walkToBank()) return 300
        if (!Bank.opened()) {
            if (!Bank.open()) {
                logger.warning("Unable to open bank.")
                return 300
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
                script.bowsBanked += bowCount
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
        return 250
    }

    private fun walkToBank(): Boolean {
        val bankTile = script.bankTile
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

