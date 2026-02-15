package org.powbot.om6.mortmyre.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Players
import org.powbot.om6.api.ScriptLogging
import org.powbot.om6.mortmyre.MortMyreFungusHarvester
import org.powbot.om6.mortmyre.Task
import org.powbot.om6.mortmyre.config.Constants

class HarvestFungusTask(script: MortMyreFungusHarvester, name: String) : Task(script, name) {

    override fun activate(): Boolean {
        if (!Inventory.isFull() && script.hasNearbyPickableFungus()) return true
        return !script.shouldReturnToPool() && script.isAtBloomTile()
    }

    override fun execute() {
        val fungi = Objects.stream()
            .name(Constants.BLOOM_OBJECT_NAME)
            .action(Constants.PICK_ACTION)
            .within(Players.local().tile(), 5.0)
            .nearest()
            .first()
        if (fungi.valid()) {
            pickFungus(fungi)
            return
        }

        if (!script.isAtBloomTile()) {
            return
        }

        if (!Inventory.opened()) {
            Inventory.open()
            Condition.wait({ Inventory.opened() }, 120, 10)
            if (!Inventory.opened()) {
                return
            }
        }

        val sickle = Inventory.stream().name(Constants.SICKLE_NAME).first()
        if (!sickle.valid()) {
            ScriptLogging.stopWithNotification(script, "Missing ${Constants.SICKLE_NAME}. Stopping script.")
            return
        }

        if (Players.local().animation() != -1) {
            Condition.sleep(150)
            return
        }

        if (sickle.click()) {
            Condition.wait(
                {
                    Objects.stream().name(Constants.BLOOM_OBJECT_NAME).action(Constants.PICK_ACTION).isNotEmpty() ||
                        script.shouldReturnToPool()
                },
                250,
                8
            )
        }
    }

    private fun pickFungus(fungi: org.powbot.api.rt4.GameObject) {
        if (Inventory.isFull()) {
            return
        }

        if (Players.local().interacting().valid()) {
            Condition.sleep(120)
            return
        }

        if (!fungi.inViewport()) {
            Camera.turnTo(fungi)
            Condition.wait({ fungi.inViewport() }, 150, 8)
            return
        }

        val countBefore = Inventory.stream().name(Constants.FUNGUS_NAME).count(true).toInt()
        if (fungi.interact(Constants.PICK_ACTION)) {
            Condition.wait(
                {
                    Inventory.stream().name(Constants.FUNGUS_NAME).count(true).toInt() > countBefore ||
                        script.shouldReturnToPool()
                },
                250,
                12
            )
            val countAfter = Inventory.stream().name(Constants.FUNGUS_NAME).count(true).toInt()
            if (countAfter > countBefore) {
                script.fungusCollected += (countAfter - countBefore)
            }
        }
    }
}
