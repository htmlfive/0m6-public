package org.powbot.community.ectofunctus.tasks

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Equipment
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.bank.Quantity
import org.powbot.community.api.WaitUtils
import org.powbot.community.ectofunctus.Ectofunctus
import org.powbot.community.ectofunctus.EctofunctusConstants.BUCKET_OF_SLIME
import org.powbot.community.ectofunctus.EctofunctusConstants.DEFAULT_BATCH_SIZE
import org.powbot.community.ectofunctus.EctofunctusConstants.DRAGON_BONES
import org.powbot.community.ectofunctus.EctofunctusConstants.EMPTY_BUCKET
import org.powbot.community.ectofunctus.EctofunctusConstants.RING_OF_DUELING
import org.powbot.community.ectofunctus.EctofunctusConstants.ECTO_TOKEN
import java.util.logging.Logger

class BankTask(private val script: Ectofunctus) : Task {
    private val logger = Logger.getLogger(BankTask::class.java.name)
    private val batchSize = script.batchSize.takeIf { it > 0 } ?: DEFAULT_BATCH_SIZE

    override fun execute(): Int {
        if (!travelToBank()) return 300
        if (!openBank()) return 300
        depositEmptyContainers()
        if (!ensureRingEquipped()) {
            logger.warning("Unable to equip a ring of dueling(8); continuing without teleport safety.")
        }
        if (!withdrawSupplies()) {
            logger.severe("Missing supplies in bank, stopping script.")
            script.controller.stop()
            return 300
        }
        Bank.close()
        Condition.wait({ !Bank.opened() }, 200, 10)
        return 250
    }

    private fun travelToBank(): Boolean {
        val bankTile = script.castleWarsBankTile
        if (isNear(bankTile)) return true

        teleportToCastleWars(bankTile)

        if (Movement.walkTo(bankTile)) {
            Condition.wait({ isNear(bankTile) }, 400, 30)
        }
        return isNear(bankTile)
    }

    private fun teleportToCastleWars(target: Tile): Boolean {
        val ring = Equipment.itemAt(Equipment.Slot.RING)
        if (!ring.valid() || !ring.name().contains("Ring of dueling", ignoreCase = true)) {
            return false
        }
        if (!ring.interact("Castle Wars")) {
            return false
        }

        val arrived = Condition.wait({ isNear(target, 15) }, 400, 30)
        if (!arrived) {
            logger.warning("Ring of dueling teleport failed, falling back to walking.")
        }
        WaitUtils.waitForLoad()
        return arrived
    }

    private fun openBank(): Boolean {
        if (Bank.opened()) return true
        val opened = Bank.open()
        if (opened) {
            Condition.wait({ Bank.opened() }, 400, 10)
        }
        return Bank.opened()
    }

    private fun depositEmptyContainers() {
        val depositBuilder = Bank.deposit()
        var shouldSubmit = false
        if (Inventory.stream().name(EMPTY_BUCKET).isNotEmpty()) {
            depositBuilder.item(EMPTY_BUCKET, Quantity.all())
            shouldSubmit = true
        }
        if (Inventory.stream().name(ECTO_TOKEN).isNotEmpty()) {
            depositBuilder.item(ECTO_TOKEN, Quantity.all())
            shouldSubmit = true
        }
        if (shouldSubmit) {
            depositBuilder.submit()
            Condition.wait({
                Inventory.stream().name(EMPTY_BUCKET).isEmpty() &&
                    Inventory.stream().name(ECTO_TOKEN).isEmpty()
            }, 300, 10)
        }
    }

    private fun ensureRingEquipped(): Boolean {
        if (isRingEquipped()) return true
        val inventoryRing = Inventory.stream().name(RING_OF_DUELING).first()
        if (inventoryRing.valid()) {
            if (inventoryRing.interact("Wear")) {
                Condition.wait({ isRingEquipped() }, 400, 10)
            }
            return isRingEquipped()
        }
        if (Bank.stream().name(RING_OF_DUELING).isEmpty()) {
            return false
        }
        if (Bank.withdraw(RING_OF_DUELING, 1)) {
            Condition.wait({ Inventory.stream().name(RING_OF_DUELING).isNotEmpty() }, 400, 10)
            return ensureRingEquipped()
        }
        return isRingEquipped()
    }

    private fun isRingEquipped(): Boolean {
        val equippedRing = Equipment.itemAt(Equipment.Slot.RING)
        return equippedRing.valid() && equippedRing.name().contains("Ring of dueling", ignoreCase = true)
    }

    private fun withdrawSupplies(): Boolean {
        val success = listOf(
            DRAGON_BONES,
            BUCKET_OF_SLIME
        ).all { withdrawRequired(it, batchSize) }
        return success
    }

    private fun withdrawRequired(name: String, amount: Int): Boolean {
        val current = Inventory.stream().name(name).count().toInt()
        if (current >= amount) return true
        val needed = amount - current
        if (!Bank.withdraw(name, needed)) {
            logger.warning("Failed to withdraw $needed x $name")
            return false
        }
        val received = Condition.wait({
            Inventory.stream().name(name).count().toInt() >= amount
        }, 400, 15)
        if (!received) {
            logger.severe("Insufficient $name. Stopping script.")
            script.controller.stop()
        }
        return received
    }

    private fun isNear(tile: Tile, distance: Int = 6): Boolean {
        return Players.local().tile().distanceTo(tile) <= distance
    }
}

