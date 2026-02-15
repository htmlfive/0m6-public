package org.powbot.community.maplefletcher.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Movement
import org.powbot.community.maplefletcher.MapleFletcherConstants
import org.powbot.community.mixology.structure.ScriptRecord
import org.powbot.community.mixology.structure.TreeTask
import java.util.logging.Logger

class WithdrawKnife(private val record: ScriptRecord) : TreeTask(true) {
    private val logger = Logger.getLogger(WithdrawKnife::class.java.name)

    override fun execute(): Int {
        if (!walkToBank()) return super.execute()
        if (!Bank.opened()) {
            if (!Bank.open()) {
                logger.warning("Unable to open bank to withdraw knife.")
                return super.execute()
            }
            Condition.wait({ Bank.opened() }, 200, 10)
        }
        if (Inventory.stream().name(MapleFletcherConstants.KNIFE_NAME).isEmpty()) {
            if (Bank.withdraw(MapleFletcherConstants.KNIFE_NAME, 1)) {
                Condition.wait(
                    { Inventory.stream().name(MapleFletcherConstants.KNIFE_NAME).isNotEmpty() },
                    200,
                    10
                )
            } else {
                logger.warning("Knife not found in bank.")
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

    override fun toString(): String = "Withdrawing knife"
}

