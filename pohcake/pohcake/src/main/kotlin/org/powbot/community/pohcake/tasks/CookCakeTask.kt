package org.powbot.community.pohcake.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Objects
import org.powbot.community.pohcake.PohCakeMaker
import org.powbot.community.pohcake.Task

private const val ITEM_UNCOOKED_CAKE = "Uncooked cake"
private const val ITEM_CAKE = "Cake"
private const val ITEM_BURNT_CAKE = "Burnt cake"
private const val RANGE_NAME = "Fancy range"

class CookCakeTask(script: PohCakeMaker) : Task(script, "Cook Cake") {

    override fun activate(): Boolean =
        script.hasUncookedCake()

    override fun execute() {
        if (Inventory.shiftDroppingEnabled()) {
            script.logger.info("Disabling tap-to-drop for cooking.")
            Inventory.disableShiftDropping()
        }
        val uncooked = Inventory.stream().name(ITEM_UNCOOKED_CAKE).first()
        if (!uncooked.valid()) {
            script.logger.warn("Uncooked cake not found in inventory.")
            return
        }

        val range = Objects.stream()
            .name(RANGE_NAME)
            .nearest()
            .first()

        if (!range.valid()) {
            script.logger.warn("Fancy range not found.")
            Condition.sleep(200)
            return
        }

        if (!range.inViewport()) {
            Camera.turnTo(range)
        }

        script.logger.info("Using uncooked cake on fancy range.")
        var attempts = 0
        while (attempts < 2 && Inventory.stream().name(ITEM_UNCOOKED_CAKE).isNotEmpty()) {
            attempts++
            if (!uncooked.click()) {
                script.logger.warn("Failed to select uncooked cake (attempt $attempts).")
                Condition.sleep(120)
                continue
            }
            Condition.wait({ Inventory.selectedItem().valid() }, 150, 20)
            if (range.click()) {
                val cooked = Condition.wait(
                    {
                        Inventory.stream().name(ITEM_UNCOOKED_CAKE).isEmpty() &&
                            Inventory.stream().name(ITEM_CAKE, ITEM_BURNT_CAKE).isNotEmpty()
                    },
                    200,
                    25
                )
                script.logger.info("Cake cooked result: $cooked")
                return
            }
            script.logger.warn("Failed to use range (attempt $attempts). Clearing selection.")
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

