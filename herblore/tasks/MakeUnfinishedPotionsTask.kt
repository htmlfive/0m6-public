package org.powbot.om6.herblore.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Skills
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.om6.herblore.HerbloreTrainer
import org.powbot.om6.herblore.data.HerbDefinition
import org.powbot.om6.herblore.data.HerbDefinitions

class MakeUnfinishedPotionsTask(script: HerbloreTrainer) : HerbloreTask(script, "Unfinished potions") {
    private companion object {
        const val PRODUCTION_WAIT_INTERVAL_MS = 200
        const val PRODUCTION_WAIT_ATTEMPTS = 150 // 30 seconds minimum
    }

    override fun shouldExecute(): Boolean {
        val config = script.runtimeConfig()
        if (config.wholeShowEnabled) {
            return script.wholeShowPhase() == HerbloreTrainer.WholeShowPhase.UNFINISHED
        }
        if (config.autoCleanAndUnfAll) {
            return !hasAnyEligibleGrimyHerbRemaining() && hasEligibleUnfinishedRecipeRemaining()
        }
        return config.mode.uiValue.equals("Unfinished potions", ignoreCase = true)
    }

    override fun execute() {
        val herb = resolveRecipeHerb()
        if (herb == null) {
            if (script.runtimeConfig().wholeShowEnabled) {
                script.advanceWholeShowToFinished()
                return
            }
            script.stopWithNotification("No unfinished potion ingredients available. Stopping script.")
            return
        }

        script.setTaskStatus("Making ${herb.displayName} potion (unf)")

        if (!hasIngredientsInInventory(herb)) {
            if (!prepareInventory(herb)) return
        }

        val water = Inventory.stream().name("Vial of water").firstOrNull()
        val cleanHerb = Inventory.stream().name(herb.cleanName).firstOrNull()
        if (water == null || cleanHerb == null || !water.valid() || !cleanHerb.valid()) {
            script.setTaskStatus("Missing ingredients after withdraw")
            return
        }

        if (!script.inventoryInteractionService.useItemOnItem(water, cleanHerb)) {
            script.setTaskStatus("Failed to use vial on herb")
            return
        }

        val clickedMake = Condition.wait(
            {
                script.widgetService.clickMakeAction()
            },
            120,
            10
        )
        if (!clickedMake) {
            script.setTaskStatus("Make widget not found")
            return
        }

        Condition.wait(
            {
                Inventory.stream().name("Vial of water").isEmpty() ||
                    Inventory.stream().name(herb.cleanName).isEmpty()
            },
            PRODUCTION_WAIT_INTERVAL_MS,
            PRODUCTION_WAIT_ATTEMPTS
        )

        if (script.bankingService.ensureOpen()) {
            script.bankingService.depositInventory()
        }
    }

    private fun prepareInventory(herb: HerbDefinition): Boolean {
        if (!script.bankingService.ensureOpen()) {
            script.setTaskStatus("Failed to open bank")
            return false
        }

        script.bankingService.depositInventory()

        if (!script.bankingService.withdraw("Vial of water", 14)) {
            script.stopWithNotification("Out of Vial of water. Stopping script.")
            return false
        }

        if (!script.bankingService.withdraw(herb.cleanName, 14)) {
            script.stopWithNotification("Out of ${herb.cleanName}. Stopping script.")
            return false
        }

        script.bankingService.ensureClosed()
        return true
    }

    private fun hasIngredientsInInventory(herb: HerbDefinition): Boolean {
        return Inventory.stream().name("Vial of water").isNotEmpty() && Inventory.stream().name(herb.cleanName).isNotEmpty()
    }

    private fun resolveRecipeHerb(): HerbDefinition? {
        val config = script.runtimeConfig()
        val level = Skills.realLevel(Skill.Herblore)
        val makeAll = config.wholeShowEnabled || config.autoCleanAndUnfAll || config.unfinishedMakeAll

        return if (makeAll) {
            HerbDefinitions.all.firstOrNull {
                level >= it.unfinishedLevel &&
                    script.bankingService.hasInBank("Vial of water") &&
                    script.bankingService.hasInBank(it.cleanName)
            }
        } else {
            val selected = config.unfinishedHerb ?: return null
            if (level < selected.unfinishedLevel) return null
            if (!script.bankingService.hasInBank("Vial of water")) return null
            if (!script.bankingService.hasInBank(selected.cleanName)) return null
            selected
        }
    }

    private fun hasAnyEligibleGrimyHerbRemaining(): Boolean {
        val level = Skills.realLevel(Skill.Herblore)
        return HerbDefinitions.all.any { level >= it.cleanLevel && script.bankingService.hasInBank(it.grimyName) } ||
            Inventory.stream().name(*HerbDefinitions.all.map { it.grimyName }.toTypedArray()).isNotEmpty()
    }

    private fun hasEligibleUnfinishedRecipeRemaining(): Boolean {
        val level = Skills.realLevel(Skill.Herblore)
        if (Inventory.stream().name("Vial of water").isNotEmpty()) {
            return HerbDefinitions.all.any { level >= it.unfinishedLevel && Inventory.stream().name(it.cleanName).isNotEmpty() }
        }
        if (!script.bankingService.hasInBank("Vial of water")) return false
        return HerbDefinitions.all.any { level >= it.unfinishedLevel && script.bankingService.hasInBank(it.cleanName) }
    }
}
