package org.powbot.om6.squidharpooner.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Players
import org.powbot.om6.squidharpooner.SquidHarpooner
import org.powbot.om6.squidharpooner.Task

private const val KNIFE_NAME = "Knife"
private const val RAW_SWORDTIP_SQUID = "Raw swordtip squid"
private const val RAW_JUMP_SQUID = "Raw jump squid"

class ProcessSquidTask(script: SquidHarpooner, name: String) : Task(script, name) {

    override fun activate(): Boolean {
        val hasSquid = script.rawSwordtipCount() > 0 || script.rawJumpCount() > 0
        if (!hasSquid) {
            return false
        }
        return Inventory.isFull() || !script.isFishingSpotWithinOneTile()
    }

    override fun execute() {
        if (!Inventory.opened()) {
            Inventory.open()
            Condition.wait({ Inventory.opened() }, 100, 10)
        }

        cutOnceAndWaitUntilNone(RAW_SWORDTIP_SQUID)

        cutOnceAndWaitUntilNone(RAW_JUMP_SQUID)

        val selected = Inventory.selectedItem()
        if (selected.valid()) {
            selected.click()
            Condition.wait({ !Inventory.selectedItem().valid() }, 80, 8)
        }
    }

    private fun cutOnceAndWaitUntilNone(fishName: String) {
        if (script.rawSquidCountByName(fishName) <= 0) {
            return
        }

        if (!ensureKnifeSelected()) {
            return
        }

        val fish = Inventory.stream().name(fishName).firstOrNull() ?: return
        if (!fish.valid()) {
            return
        }

        if (!fish.click()) {
            return
        }

        Condition.wait(
            {
                Players.local().animation() != -1 || script.rawSquidCountByName(fishName) == 0
            },
            120,
            20
        )

        Condition.wait(
            {
                script.rawSquidCountByName(fishName) == 0
            },
            120,
            200
        )
    }

    private fun ensureKnifeSelected(): Boolean {
        val knife = Inventory.stream().name(KNIFE_NAME).firstOrNull() ?: return false
        if (!knife.valid()) {
            return false
        }

        val selected = Inventory.selectedItem()
        if (selected.valid() && selected.id() == knife.id()) {
            return true
        }

        if (selected.valid() && selected.id() != knife.id()) {
            selected.click()
            Condition.wait({ !Inventory.selectedItem().valid() }, 60, 6)
        }

        if (!knife.interact("Use")) {
            return false
        }

        return Condition.wait({ Inventory.selectedItem().id() == knife.id() }, 80, 10)
    }
}
