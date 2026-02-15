package org.powbot.om6.api

import org.powbot.api.Condition
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.Components
import org.powbot.api.rt4.GameObject
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Poh
import org.powbot.api.rt4.Widgets

object PohJewelryBoxUtils {
    private const val JEWELRY_BOX_WIDGET_ID = 590
    private const val JEWELRY_BOX_MENU_ACTION = "Teleport Menu"

    fun ensureInsidePoh(
        teleportItemName: String,
        onWarn: (String) -> Unit = {},
        onInfo: (String) -> Unit = {}
    ): Boolean {
        if (Poh.inside()) {
            return true
        }
        if (!ConsumableUtils.hasEmergencyTeleportItem(teleportItemName)) {
            onWarn("Teleport to house required for POH travel but not found.")
            return false
        }
        onInfo("Using Teleport to house.")
        if (!ConsumableUtils.useEmergencyTeleportItem(teleportItemName)) {
            onWarn("Failed to use Teleport to house.")
            return false
        }
        val entered = Condition.wait({ Poh.inside() }, 250, 20)
        if (!entered) {
            onWarn("Teleport to house did not complete.")
        }
        return entered
    }

    fun findJewelryBox(jewelryBoxName: String): GameObject? {
        val jewelryBox = Objects.stream()
            .filtered { it.name().equals(jewelryBoxName, true) }
            .nearest()
            .firstOrNull()
        return if (jewelryBox != null && jewelryBox.valid()) jewelryBox else null
    }

    fun ensureVisible(jewelryBox: GameObject) {
        if (jewelryBox.inViewport()) {
            return
        }
        org.powbot.api.rt4.Movement.step(jewelryBox.tile())
        Condition.wait({ jewelryBox.inViewport() }, 200, 10)
        Camera.turnTo(jewelryBox)
    }

    fun useTeleportOption(
        jewelryBox: GameObject,
        destinationOption: String,
        onWarn: (String) -> Unit = {},
        onInfo: (String) -> Unit = {}
    ): Boolean {
        val hasDirectAction = jewelryBox.actions().any { it.equals(destinationOption, true) }
        if (hasDirectAction) {
            onInfo("Using cached jewellery box action: $destinationOption")
            if (jewelryBox.interact(destinationOption)) {
                return true
            }
            onWarn("Failed to use cached action $destinationOption, falling back to Teleport Menu.")
        }

        if (!jewelryBox.interact(JEWELRY_BOX_MENU_ACTION)) {
            onWarn("Failed to open jewellery box menu.")
            return false
        }

        val menuOpened = Condition.wait({
            Widgets.widget(JEWELRY_BOX_WIDGET_ID).valid() &&
                Components.stream(JEWELRY_BOX_WIDGET_ID)
                    .filtered { it.visible() }
                    .firstOrNull() != null
        }, 200, 15)
        if (!menuOpened) {
            onWarn("Jewellery box menu did not open.")
            return false
        }

        val destinationComponent = Components.stream(JEWELRY_BOX_WIDGET_ID)
            .filtered { component ->
                component.visible() &&
                    (component.text().contains(destinationOption, true) ||
                        component.name().contains(destinationOption, true))
            }
            .firstOrNull()
        if (destinationComponent == null || !destinationComponent.valid()) {
            onWarn("$destinationOption option not found in jewellery box menu.")
            return false
        }
        if (!destinationComponent.click()) {
            onWarn("Failed to select $destinationOption from jewellery box menu.")
            return false
        }
        return true
    }

    fun waitForPohExitAfterTeleport(
        jewelryBox: GameObject,
        onWarn: (String) -> Unit = {}
    ): Boolean {
        val boxTile = jewelryBox.tile()
        val leftBox = Condition.wait({
            val distance = Players.local().tile().distanceTo(boxTile)
            distance > 5 || !Poh.inside()
        }, 150, 40)

        if (!leftBox) {
            onWarn("Player still near jewellery box after teleport attempt.")
            return false
        }

        WaitUtils.waitForLoad()

        val exitedHouse = Condition.wait({ !Poh.inside() }, 200, 40)
        if (!exitedHouse) {
            onWarn("Jewellery box teleport did not leave the POH.")
            return false
        }

        return true
    }
}
