package org.powbot.om6.herbrun.services

import org.powbot.api.Condition
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.GameObject
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Players
import org.powbot.om6.api.MovementUtils
import org.powbot.om6.herbrun.HerbRun
import kotlin.random.Random

private const val LIMPWURT_SEED_NAME = "Limpwurt seed"
private const val FLOWER_PATCH_OBJECT_ID = 33649
private val FORCED_FLOWER_FLOW_OBJECT_IDS = setOf(7840, 7841, FLOWER_PATCH_OBJECT_ID)

class LimpwurtService(private val script: HerbRun) {
    private var lastDiagAtMillis = 0L

    fun handleDuringPickpocket(): Boolean {
        if (!script.shouldPickpocketBetweenRuns()) {
            logDiag("Skipped: not in between-run cooldown phase.")
            return false
        }

        val farmerTile = script.limpwurtMasterFarmerTile()
        val deadPatch = findNearbyDeadFlowerPatch()
        val limpwurtPlant = findNearbyLimpwurtPlant()
        val flowerPatch = findNearbyFlowerPatch()
        val flowerNamedPatch = flowerPatch?.name()?.equals("Flower Patch", ignoreCase = true) == true
        val forcedFlowId = flowerPatch?.id in FORCED_FLOWER_FLOW_OBJECT_IDS
        val flowerHasRake = flowerPatch != null && flowerPatch.valid() && script.limpwurtHasObjectAction(flowerPatch, "Rake")
        val flowerHasPlant = flowerPatch != null && flowerPatch.valid() && script.limpwurtHasObjectAction(flowerPatch, "Plant")
        val flowerHasCompost = flowerPatch != null && flowerPatch.valid() && script.limpwurtHasObjectAction(flowerPatch, "Compost")
        val flowerHasUse = flowerPatch != null && flowerPatch.valid() && script.limpwurtHasObjectAction(flowerPatch, "Use")

        val needsFlow =
            (deadPatch != null && deadPatch.valid()) ||
                (limpwurtPlant != null && limpwurtPlant.valid() && script.limpwurtHasObjectAction(limpwurtPlant, "Pick")) ||
                flowerNamedPatch ||
                forcedFlowId ||
                (flowerPatch != null && flowerPatch.valid() && (flowerHasRake || flowerHasPlant || flowerHasCompost || flowerHasUse))

        logDiag(
            "Scan: dead=${deadPatch?.valid() == true} limpwurtPick=${limpwurtPlant?.valid() == true && script.limpwurtHasObjectAction(limpwurtPlant, "Pick")} " +
                "flower=${flowerPatch?.valid() == true} flowerId=${flowerPatch?.id ?: -1} " +
                "namedPatch=$flowerNamedPatch forcedId=$forcedFlowId rake=$flowerHasRake plant=$flowerHasPlant compost=$flowerHasCompost use=$flowerHasUse needsFlow=$needsFlow"
        )
        if (!needsFlow) {
            return false
        }

        if (!hasLimpwurtSupplies()) {
            logDiag(
                "Needs supplies: secateurs=${Inventory.stream().name("Magic secateurs").isNotEmpty()} " +
                    "spade=${Inventory.stream().name("Spade").isNotEmpty()} " +
                    "rake=${Inventory.stream().name("Rake").isNotEmpty()} " +
                    "seed=${Inventory.stream().name(LIMPWURT_SEED_NAME).isNotEmpty()} " +
                    "compost=${script.limpwurtCompostName()?.let { Inventory.stream().name(it).isNotEmpty() } ?: true}"
            )
            script.limpwurtSetTask("Limpwurt banking")
            performBanking()
            return true
        }

        if (!script.limpwurtIsNearTile(farmerTile)) {
            script.limpwurtSetTask("Walking to flower patch")
            MovementUtils.enableRunIfNeeded()
            Movement.walkTo(farmerTile)
            Condition.wait({ script.limpwurtIsNearTile(farmerTile) }, 300, 20)
            return true
        }

        if (Bank.opened()) {
            Bank.close()
            Condition.wait({ !Bank.opened() }, 120, 12)
            return true
        }

        val activeDeadPatch = findNearbyDeadFlowerPatch()
        if (activeDeadPatch != null && activeDeadPatch.valid()) {
            script.limpwurtSetTask("Clearing dead flower patch")
            if (activeDeadPatch.interact("Clear")) {
                Condition.wait({ !activeDeadPatch.valid() || !activeDeadPatch.name().contains("dead", ignoreCase = true) }, 300, 20)
            }
            return true
        }

        val activeLimpwurtPlant = findNearbyLimpwurtPlant()
        if (activeLimpwurtPlant != null && activeLimpwurtPlant.valid() && script.limpwurtHasObjectAction(activeLimpwurtPlant, "Pick")) {
            script.limpwurtSetTask("Picking limpwurt plant")
            if (activeLimpwurtPlant.interact("Pick")) {
                Condition.wait({ Players.local().animation() != -1 }, 150, 20)
                Condition.wait({ Players.local().animation() == -1 }, 150, 30)
                Condition.wait({ !script.limpwurtHasObjectAction(activeLimpwurtPlant, "Pick") || !activeLimpwurtPlant.valid() }, 300, 20)
            }
            return true
        }

        val activeFlowerPatch = findNearbyFlowerPatch()
        if (activeFlowerPatch == null || !activeFlowerPatch.valid()) {
            script.limpwurtSetTask("Waiting for flower patch")
            logDiag("No valid flower patch found near $farmerTile while limpwurt flow active.")
            Condition.sleep(Random.nextInt(180, 320))
            return true
        }

        if (script.limpwurtHasObjectAction(activeFlowerPatch, "Rake")) {
            script.limpwurtSetTask("Raking flower patch")
            if (activeFlowerPatch.interact("Rake")) {
                Condition.wait({ !script.limpwurtHasObjectAction(activeFlowerPatch, "Rake") || Players.local().animation() != -1 }, 200, 30)
                Condition.wait({ Players.local().animation() == -1 }, 200, 30)
            }
            return true
        }

        if (script.limpwurtHasObjectAction(activeFlowerPatch, "Plant")) {
            script.limpwurtSetTask("Planting limpwurt seed")
            val seed = Inventory.stream().name(LIMPWURT_SEED_NAME).firstOrNull()
            if (seed == null) {
                script.limpwurtWarn("Missing $LIMPWURT_SEED_NAME during limpwurt cycle.")
                return true
            }
            if (!seed.interact("Use")) {
                logDiag("Failed to use limpwurt seed item before patch interaction.")
                return true
            }
            Condition.sleep(Random.nextInt(220, 360))
            val plantedClick = activeFlowerPatch.interact("Use")
            logDiag("Plant click result=$plantedClick on flowerPatchId=${activeFlowerPatch.id}.")
            Condition.wait({
                val refreshed = findNearbyFlowerPatch()
                refreshed == null || !refreshed.valid() || !script.limpwurtHasObjectAction(refreshed, "Plant")
            }, 300, 20)

            logDiag(
                "Post-plant refresh: flowerPresent=${findNearbyFlowerPatch()?.valid() == true} " +
                    "canPlant=${findNearbyFlowerPatch()?.let { script.limpwurtHasObjectAction(it, "Plant") } ?: false} " +
                    "canCompost=${findNearbyFlowerPatch()?.let { script.limpwurtHasObjectAction(it, "Compost") || script.limpwurtHasObjectAction(it, "Use") } ?: false}"
            )

            val refreshedPatch = findNearbyFlowerPatch()
            if (refreshedPatch != null &&
                refreshedPatch.valid() &&
                (script.limpwurtHasObjectAction(refreshedPatch, "Compost") || script.limpwurtHasObjectAction(refreshedPatch, "Use"))
            ) {
                script.limpwurtSetTask("Composting flower patch")
                val composted = compostOnLimpwurtPlantAfterPlanting()
                logDiag("Post-plant compost-on-limpwurt result=$composted.")
            }
            return true
        }

        if (script.limpwurtHasObjectAction(activeFlowerPatch, "Compost") || script.limpwurtHasObjectAction(activeFlowerPatch, "Use")) {
            script.limpwurtSetTask("Composting flower patch")
            val composted = script.limpwurtApplyCompost(activeFlowerPatch)
            logDiag("Direct compost result=$composted on flowerPatchId=${activeFlowerPatch.id}.")
            return true
        }

        // Some flower patch states (e.g. 7840/7841) expose no actions; force seed + compost flow anyway.
        if (activeFlowerPatch.id in FORCED_FLOWER_FLOW_OBJECT_IDS || activeFlowerPatch.name().equals("Flower Patch", ignoreCase = true)) {
            script.limpwurtSetTask("Forcing limpwurt flow")
            val planted = forcePlantWithoutAction(activeFlowerPatch)
            if (planted) {
                val composted = compostOnLimpwurtPlantAfterPlanting()
                logDiag("Forced flow result: planted=$planted compostedOnLimpwurt=$composted flowerPatchId=${activeFlowerPatch.id}.")
            } else {
                logDiag("Forced flow plant failed on flowerPatchId=${activeFlowerPatch.id}; holding limpwurt flow.")
            }
            return true
        }

        return true
    }

    private fun performBanking() {
        if (!script.limpwurtIsNearTile(script.limpwurtBankTile())) {
            if (Bank.opened()) {
                Bank.close()
            }
            MovementUtils.enableRunIfNeeded()
            Movement.walkTo(script.limpwurtBankTile())
            Condition.wait({ script.limpwurtIsNearTile(script.limpwurtBankTile()) }, 300, 25)
            return
        }

        if (!Bank.opened()) {
            script.limpwurtOpenBank("limpwurt supplies")
            return
        }

        if (!script.limpwurtIsNearGuildBank()) {
            Bank.close()
            return
        }

        if (!script.limpwurtDepositInventory()) {
            return
        }

        val supplies = mutableListOf(
            "Magic secateurs" to 1,
            "Spade" to 1,
            "Rake" to 1,
            LIMPWURT_SEED_NAME to 1
        )
        val compostName = script.limpwurtCompostName()
        if (!compostName.isNullOrBlank()) {
            supplies.add(compostName to 1)
        }

        for ((name, amount) in supplies) {
            if (Inventory.stream().name(name).count(true).toInt() >= amount) {
                continue
            }
            if (!script.limpwurtWithdraw(name, amount)) {
                val reason = "Missing required limpwurt supply: $name x$amount"
                script.limpwurtError(reason)
                script.limpwurtStop(reason)
                return
            }
        }

        Bank.close()
        Condition.wait({ !Bank.opened() }, 120, 12)
    }

    private fun hasLimpwurtSupplies(): Boolean {
        val hasTools = Inventory.stream().name("Magic secateurs").isNotEmpty() &&
            Inventory.stream().name("Spade").isNotEmpty() &&
            Inventory.stream().name("Rake").isNotEmpty()
        if (!hasTools) return false
        if (Inventory.stream().name(LIMPWURT_SEED_NAME).isEmpty()) return false
        val compostName = script.limpwurtCompostName()
        if (compostName.isNullOrBlank()) return true
        return Inventory.stream().name(compostName).isNotEmpty()
    }

    private fun findNearbyLimpwurtPlant(): GameObject? =
        Objects.stream()
            .name("Limpwurt plant")
            .within(script.limpwurtMasterFarmerTile(), 12.0)
            .nearest()
            .firstOrNull()

    private fun findNearbyFlowerPatch(): GameObject? =
        Objects.stream()
            .filtered { obj ->
                val name = obj.name()
                obj.id == FLOWER_PATCH_OBJECT_ID ||
                    name.equals("Flower Patch", ignoreCase = true) ||
                    name.equals("Flower patch", ignoreCase = true) ||
                    name.contains("flower patch", ignoreCase = true)
            }
            .within(script.limpwurtMasterFarmerTile(), 12.0)
            .nearest()
            .firstOrNull()

    private fun findNearbyDeadFlowerPatch(): GameObject? =
        Objects.stream()
            .filtered { obj -> obj.name().contains("dead", ignoreCase = true) && script.limpwurtHasObjectAction(obj, "Clear") }
            .within(script.limpwurtMasterFarmerTile(), 12.0)
            .nearest()
            .firstOrNull()

    private fun logDiag(message: String, throttleMs: Long = 1200L) {
        val now = System.currentTimeMillis()
        if (now - lastDiagAtMillis < throttleMs) {
            return
        }
        lastDiagAtMillis = now
        script.limpwurtInfo("LIMPWURT: $message")
    }

    private fun forcePlantWithoutAction(patch: GameObject): Boolean {
        val seed = Inventory.stream().name(LIMPWURT_SEED_NAME).firstOrNull()
        if (seed == null) {
            script.limpwurtWarn("Missing $LIMPWURT_SEED_NAME during forced limpwurt flow.")
            return false
        }
        if (!seed.interact("Use")) {
            return false
        }
        Condition.sleep(Random.nextInt(220, 360))
        val clicked = patch.interact("Use")
        if (!clicked) {
            return false
        }
        Condition.wait({ Players.local().animation() != -1 }, 150, 20)
        Condition.wait({ Players.local().animation() == -1 }, 150, 30)
        return true
    }

    private fun compostOnLimpwurtPlantAfterPlanting(): Boolean {
        script.limpwurtSetTask("Waiting for limpwurt plant")
        val plantAppeared = Condition.wait(
            { findNearbyLimpwurtPlant()?.valid() == true },
            200,
            25
        )
        if (!plantAppeared) {
            logDiag("Compost step skipped: limpwurt plant did not appear in time.")
            return false
        }

        var plant = findNearbyLimpwurtPlant() ?: return false
        if (!plant.valid()) {
            return false
        }

        val distance = Players.local().tile().distanceTo(plant.tile())
        if (distance > 2.0) {
            script.limpwurtSetTask("Walking to limpwurt plant")
            MovementUtils.enableRunIfNeeded()
            Movement.walkTo(plant.tile())
            Condition.wait(
                {
                    val refreshed = findNearbyLimpwurtPlant()
                    refreshed != null && refreshed.valid() && Players.local().tile().distanceTo(refreshed.tile()) <= 2.0
                },
                200,
                20
            )
            plant = findNearbyLimpwurtPlant() ?: return false
            if (!plant.valid()) {
                return false
            }
        }

        script.limpwurtSetTask("Composting limpwurt plant")
        return script.limpwurtApplyCompost(plant)
    }
}
