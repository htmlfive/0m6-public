package org.powbot.community.pohcake.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Inventory
import org.powbot.community.pohcake.PohCakeMaker
import org.powbot.community.pohcake.Task

private const val ITEM_EGG = "Egg"
private const val ITEM_CAKE_TIN = "Cake tin"
private const val ITEM_UNCOOKED_CAKE = "Uncooked cake"

class CombineIngredientsTask(script: PohCakeMaker) : Task(script, "Combine Ingredients") {

    override fun activate(): Boolean =
        script.hasAllIngredients() && script.hasCakeTin() && !script.hasUncookedCake()

    override fun execute() {
        if (Inventory.shiftDroppingEnabled()) {
            script.logger.info("Disabling tap-to-drop for combining.")
            Inventory.disableShiftDropping()
        }
        if (!Inventory.opened()) {
            Inventory.open()
            Condition.wait({ Inventory.opened() }, 120, 10)
        }

        val egg = Inventory.stream().name(ITEM_EGG).first()
        val cakeTin = Inventory.stream().name(ITEM_CAKE_TIN).first()
        if (!egg.valid() || !cakeTin.valid()) {
            script.logger.warn("Missing egg or cake tin for combining.")
            return
        }

        script.logger.info("Using egg on cake tin.")
        var attempts = 0
        while (attempts < 2 && Inventory.stream().name(ITEM_UNCOOKED_CAKE).isEmpty()) {
            attempts++
            if (!egg.click()) {
                script.logger.warn("Failed to select egg (attempt $attempts).")
                Condition.sleep(120)
                continue
            }
            Condition.wait({ Inventory.selectedItem().valid() }, 150, 20)
            if (cakeTin.click()) {
                val made = Condition.wait(
                    { Inventory.stream().name(ITEM_UNCOOKED_CAKE).isNotEmpty() },
                    150,
                    20
                )
                script.logger.info("Uncooked cake created: $made")
                return
            }
            script.logger.warn("Failed to use cake tin (attempt $attempts). Clearing selection.")
            clearSelectedItem()
            Condition.sleep(120)
        }
    }

    private fun clearSelectedItem() {
        val selected = Inventory.selectedItem()
        if (selected.valid()) {
            selected.click()
            Condition.wait({ !Inventory.selectedItem().valid() }, 120, 10)
        }
    }
}

