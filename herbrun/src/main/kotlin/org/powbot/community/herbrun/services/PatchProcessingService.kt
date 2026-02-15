package org.powbot.community.herbrun.services

import org.powbot.api.Condition
import org.powbot.api.rt4.GameObject
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Item
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Players
import org.powbot.community.api.MovementUtils
import org.powbot.community.herbrun.HerbRun
import org.powbot.community.herbrun.config.HerbPatch
import kotlin.random.Random

class PatchProcessingService(private val script: HerbRun) {

    fun handleWeeding(patch: HerbPatch, patchObject: GameObject) {
        if (rakePatch(patch, patchObject)) {
            script.patchResetStatus(patch)
            val refreshed = script.patchFindPatchObject(patch)
            if (refreshed != GameObject.Nil) {
                val refreshedState = script.patchDetermineState(refreshed)
                when (refreshedState) {
                    HerbRun.PatchState.EMPTY_SOIL -> handleEmptyPatch(patch, refreshed)
                    HerbRun.PatchState.GROWING -> {
                        val status = script.patchStatus(patch)
                        if (status?.seedPlanted == true) {
                            script.patchInfo("${patch.displayName}: Fresh herbs planted. Moving to next patch.")
                        } else {
                            script.patchInfo("${patch.displayName}: Herbs still growing. Nothing to do.")
                        }
                        script.patchFinishPatch(patch)
                    }
                    HerbRun.PatchState.READY_TO_HARVEST -> harvestPatch(patch, refreshed)
                    HerbRun.PatchState.DEAD -> clearDeadPatch(patch, refreshed)
                    HerbRun.PatchState.NEEDS_WEEDING,
                    HerbRun.PatchState.UNKNOWN -> {
                        script.patchDebug("${patch.displayName}: Patch state unknown after raking, waiting...")
                        Condition.sleep(400)
                    }
                }
            }
        }
    }

    fun handleEmptyPatch(patch: HerbPatch, patchObject: GameObject) {
        val status = script.patchStatus(patch) ?: return
        ensureAtPatchBeforeSetup(patch)

        if (script.patchHasCompostName() && !status.compostApplied) {
            if (applyCompost(patch, patchObject)) {
                status.compostApplied = true
            } else {
                return
            }
        }

        if (!status.seedPlanted) {
            if (plantSeeds(patch, patchObject)) {
                status.seedPlanted = true
                script.patchFinishPatch(patch)
            }
            return
        }
    }

    fun harvestPatch(patch: HerbPatch, patchObject: GameObject) {
        val produceName = script.patchProduceName()
        var totalGained = 0
        var attempts = 0

        while (attempts < 12) {
            attempts++
            val before = countHerbProduceInInventory()
            script.patchInfo("${patch.displayName}: Picking herbs.")
            val interacted =
                patchObject.interact("Pick") || patchObject.interact("Harvest") || patchObject.interact("Pick herb")
            if (!interacted) {
                script.patchWarn("${patch.displayName}: Unable to harvest herb patch.")
                break
            }

            Condition.wait(
                {
                    Players.local().animation() != -1 ||
                        countHerbProduceInInventory() > before
                },
                120,
                12
            )
            Condition.wait(
                { Players.local().animation() == -1 },
                400,
                20
            )

            val after = countHerbProduceInInventory()
            val gained = (after - before).coerceAtLeast(0)
            totalGained += gained

            if (Inventory.isFull() && script.patchShouldNoteHarvest()) {
                script.patchInfo("${patch.displayName}: Inventory full, noting herbs.")
                script.patchNoteHerbs(patch)
            }

            val state = script.patchDetermineState(script.patchFindPatchObject(patch))
            if (state != HerbRun.PatchState.READY_TO_HARVEST) {
                break
            }
        }

        if (totalGained > 0) {
            script.patchAddHerbsHarvested(totalGained)
            script.patchInfo("${patch.displayName}: Picked $totalGained ${produceName.lowercase()}.")
        }
        if (script.patchShouldNoteHarvest()) {
            script.patchInfo("${patch.displayName}: Noting remaining herbs after harvest.")
            script.patchNoteHerbs(patch)
        }
    }

    fun clearDeadPatch(patch: HerbPatch, patchObject: GameObject) {
        script.patchInfo("${patch.displayName}: Clearing dead herbs.")
        if (!patchObject.interact("Clear")) {
            return
        }
        Condition.wait(
            { script.patchDetermineState(script.patchFindPatchObject(patch)) != HerbRun.PatchState.DEAD },
            400,
            20
        )
        script.patchResetStatus(patch)
        script.patchInfo("${patch.displayName}: Cleared dead herbs.")
    }

    private fun rakePatch(patch: HerbPatch, patchObject: GameObject): Boolean {
        script.patchInfo("${patch.displayName}: Raking patch.")
        if (!patchObject.interact("Rake")) {
            return false
        }
        Condition.wait(
            { script.patchDetermineState(script.patchFindPatchObject(patch)) != HerbRun.PatchState.NEEDS_WEEDING },
            400,
            20
        )
        return true
    }

    private fun applyCompost(patch: HerbPatch, patchObject: GameObject): Boolean {
        val compostName = script.patchCompostItemName() ?: return true
        val compostItem = Inventory.stream().name(compostName).firstOrNull()
        if (compostItem == null) {
            script.patchWarn("Missing compost: $compostName")
            return false
        }
        val beforeCount = if (patch == HerbPatch.PORT_PHASMATYS) {
            Inventory.stream().name(compostName).count(true).toInt()
        } else {
            -1
        }

        script.patchInfo("Using compost: $compostName")
        if (!compostItem.interact("Use")) {
            return false
        }
        Condition.sleep(Random.nextInt(200, 350))

        script.patchInfo("Applying compost to patch.")
        if (!patchObject.interact("Use")) {
            return false
        }

        if (patch == HerbPatch.PORT_PHASMATYS) {
            val consumed = Condition.wait(
                { Inventory.stream().name(compostName).count(true).toInt() < beforeCount },
                200,
                75
            )
            if (!consumed) {
                script.patchWarn("${patch.displayName}: Compost count did not decrease; retrying setup tick.")
                return false
            }
            Condition.sleep(Random.nextInt(1200, 1501))
            script.patchInfo("Applied $compostName")
            return true
        }

        Condition.wait(
            { Players.local().animation() != -1 },
            200,
            75
        )
        Condition.wait(
            { Players.local().animation() == -1 },
            200,
            75
        )
        script.patchInfo("Applied $compostName")
        return true
    }

    private fun plantSeeds(patch: HerbPatch, patchObject: GameObject): Boolean {
        val seed = selectSeedForPlanting()
        if (seed == null) {
            script.patchError("${patch.displayName}: No configured seeds found in inventory.")
            return false
        }
        val seedItem = Inventory.stream().name(seed).firstOrNull()
        if (seedItem == null) {
            script.patchError("No $seed found in inventory.")
            return false
        }
        val beforeSeedCount = if (patch == HerbPatch.PORT_PHASMATYS) {
            Inventory.stream().name(seed).count(true).toInt()
        } else {
            -1
        }

        script.patchInfo("${patch.displayName}: Using seed $seed.")
        if (!seedItem.interact("Use")) {
            return false
        }
        Condition.sleep(Random.nextInt(220, 360))

        script.patchInfo("${patch.displayName}: Planting seed on patch.")
        if (!patchObject.interact("Use")) {
            return false
        }

        val planted = if (patch == HerbPatch.PORT_PHASMATYS) {
            Condition.wait(
                { Inventory.stream().name(seed).count(true).toInt() < beforeSeedCount },
                200,
                75
            )
        } else {
            Condition.wait(
                { script.patchDetermineState(script.patchFindPatchObject(patch)) == HerbRun.PatchState.GROWING },
                400,
                20
            )
        }

        if (planted) {
            if (patch == HerbPatch.PORT_PHASMATYS) {
                Condition.sleep(Random.nextInt(1200, 1501))
            }
            script.patchInfo("${patch.displayName}: Planted $seed.")
        }

        return planted
    }

    private fun ensureAtPatchBeforeSetup(patch: HerbPatch) {
        if (Players.local().tile().distanceTo(patch.tile) <= 10.0) {
            return
        }
        script.patchInfo("${patch.displayName}: Returning to herb patch tile before compost/plant.")
        MovementUtils.enableRunIfNeeded()
        Movement.walkTo(patch.tile)
        Condition.wait({ Players.local().tile().distanceTo(patch.tile) <= 10.0 }, 250, 20)
    }

    private fun selectSeedForPlanting(): String? {
        return script.patchSeedPriorityItemNames().firstOrNull { Inventory.stream().name(it).count(true).toInt() > 0 }
    }

    private fun countHerbProduceInInventory(): Int {
        return Inventory.stream()
            .filtered { isCleanableHerb(it) }
            .count(true)
            .toInt()
    }

    private fun isCleanableHerb(item: Item): Boolean {
        val actions = item.actions()
        return actions.any { it.equals("Clean", ignoreCase = true) }
    }
}

