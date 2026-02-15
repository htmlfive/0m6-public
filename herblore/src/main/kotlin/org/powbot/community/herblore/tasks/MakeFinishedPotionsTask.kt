package org.powbot.community.herblore.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Skills
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.community.herblore.HerbloreTrainer
import org.powbot.community.herblore.data.FinishedPotionRecipe
import org.powbot.community.herblore.data.FinishedPotionRecipes

class MakeFinishedPotionsTask(script: HerbloreTrainer) : HerbloreTask(script, "Finished potions") {
    private companion object {
        const val PRODUCTION_WAIT_INTERVAL_MS = 200
        const val PRODUCTION_WAIT_ATTEMPTS = 150 // 30 seconds minimum
    }

    override fun shouldExecute(): Boolean {
        val config = script.runtimeConfig()
        if (config.wholeShowEnabled) {
            return script.wholeShowPhase() == HerbloreTrainer.WholeShowPhase.FINISHED
        }
        return config.mode.uiValue.equals("Finished potions", ignoreCase = true)
    }

    override fun execute() {
        val recipe = resolveRecipeToMake()
        if (recipe == null) {
            if (script.runtimeConfig().wholeShowEnabled) {
                script.completeWholeShow()
            } else if (script.runtimeConfig().finishedQueueEnabled) {
                script.stopWithNotification("Finished potion queue complete (no supplies left). Stopping script.")
            } else {
                script.setTaskStatus("No finished potion selected")
                Condition.sleep(400)
            }
            return
        }

        val herbloreLevel = Skills.realLevel(Skill.Herblore)
        if (herbloreLevel < recipe.level) {
            script.setTaskStatus("Need Herblore ${recipe.level} for ${recipe.displayName}")
            Condition.sleep(400)
            return
        }

        script.setTaskStatus("Making ${recipe.displayName}")

        if (!hasIngredientsInInventory(recipe)) {
            if (!prepareInventory(recipe)) {
                if (script.runtimeConfig().finishedQueueEnabled) {
                    script.setTaskStatus("Skipping ${recipe.displayName} (missing ingredients)")
                } else {
                    script.stopWithNotification("Out of ${recipe.unfinishedPotionName} or ${recipe.secondaryName}. Stopping script.")
                }
                return
            }
        }

        val unfinished = Inventory.stream().name(recipe.unfinishedPotionName).firstOrNull()
        val secondary = Inventory.stream().name(recipe.secondaryName).firstOrNull()
        if (unfinished == null || secondary == null || !unfinished.valid() || !secondary.valid()) {
            script.setTaskStatus("Missing ingredients after withdraw")
            return
        }

        if (!script.inventoryInteractionService.useItemOnItem(unfinished, secondary)) {
            script.setTaskStatus("Failed to use item on item")
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
                Inventory.stream().name(recipe.unfinishedPotionName).isEmpty() ||
                    Inventory.stream().name(recipe.secondaryName).isEmpty()
            },
            PRODUCTION_WAIT_INTERVAL_MS,
            PRODUCTION_WAIT_ATTEMPTS
        )

        if (script.bankingService.ensureOpen()) {
            script.bankingService.depositInventory()
        }
    }

    private fun prepareInventory(recipe: FinishedPotionRecipe): Boolean {
        if (!script.bankingService.ensureOpen()) {
            script.setTaskStatus("Failed to open bank")
            return false
        }

        script.bankingService.depositInventory()

        script.bankingService.withdraw(recipe.unfinishedPotionName, 14)
        script.bankingService.withdraw(recipe.secondaryName, 14)

        val unfCount = Inventory.stream().name(recipe.unfinishedPotionName).count(true).toInt()
        val secondaryCount = Inventory.stream().name(recipe.secondaryName).count(true).toInt()

        script.bankingService.ensureClosed()
        return unfCount > 0 && secondaryCount > 0
    }

    private fun hasIngredientsInInventory(recipe: FinishedPotionRecipe): Boolean {
        return Inventory.stream().name(recipe.unfinishedPotionName).isNotEmpty() &&
            Inventory.stream().name(recipe.secondaryName).isNotEmpty()
    }

    private fun resolveRecipeToMake(): FinishedPotionRecipe? {
        val config = script.runtimeConfig()
        if (!config.finishedQueueEnabled) {
            return config.finishedPotion
        }

        val herbloreLevel = Skills.realLevel(Skill.Herblore)
        val selected = FinishedPotionRecipes.supported.filter { config.finishedQueueSelections.contains(it.displayName) }
        return selected.firstOrNull { herbloreLevel >= it.level && hasAnySuppliesFor(it) }
    }

    private fun hasAnySuppliesFor(recipe: FinishedPotionRecipe): Boolean {
        val unfInventory = Inventory.stream().name(recipe.unfinishedPotionName).count(true).toInt()
        val secondaryInventory = Inventory.stream().name(recipe.secondaryName).count(true).toInt()
        val unfBank = Bank.stream().name(recipe.unfinishedPotionName).count(true).toInt()
        val secondaryBank = Bank.stream().name(recipe.secondaryName).count(true).toInt()
        return (unfInventory + unfBank) > 0 && (secondaryInventory + secondaryBank) > 0
    }

}

