package org.powbot.om6.baggedplants.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.Chat
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Npcs
import org.powbot.api.rt4.Poh
import org.powbot.api.waiter.TickWaiter
import org.powbot.om6.baggedplants.BaggedPlants

class RefillTask(script: BaggedPlants) : BaggedPlants.Task(script) {

    override fun activate(): Boolean = !hasUnnotedBaggedPlants()

    override fun execute() {
        val insideHouse = Poh.inside()
        val needCans = !hasFullWateringCans()
        val needPlants = !hasUnnotedBaggedPlants()

        if (insideHouse && (needCans || needPlants)) {
            script.logger.info("REFILL: Missing supplies, exiting house")
            leaveHouse()
            return
        }

        if (!hasFullWateringCans()) {
            if (!refillWateringCans()) {
                return
            }
        }

        if (hasFullWateringCans() && !hasUnnotedBaggedPlants()) {
            if (!unnoteBaggedPlants()) {
                return
            }
        }

        if (!Poh.inside() && hasFullWateringCans() && hasUnnotedBaggedPlants()) {
            reenterHouse()
        }
    }

    private fun leaveHouse(): Boolean {
        val portal = Objects.stream().name("Portal").action("Enter").nearest().first()
        if (!portal.valid()) {
            script.logger.warn("REFILL: Portal to leave house not found")
            return false
        }

        if (!portal.interact("Enter")) {
            script.logger.warn("REFILL: Failed to click portal when leaving")
            return false
        }

        if (!Condition.wait({ !Poh.inside() }, 300, 15)) {
            script.logger.warn("REFILL: Still inside after using portal")
            return false
        }

        TickWaiter(2).wait()
        return true
    }

    private fun refillWateringCans(): Boolean {
        val needsRefill = !hasFullWateringCans()
        if (!needsRefill) {
            return true
        }

        if (!Condition.wait({ Objects.stream().name("Well").isNotEmpty() }, 300, 15)) {
            script.logger.warn("REFILL: Well not found nearby")
            return false
        }

        val wateringCan = Inventory.stream().nameContains("Watering can")
            .filtered { !it.name().equals("Watering can(8)", ignoreCase = true) }
            .first()
        if (!wateringCan.valid()) {
            script.logger.warn("REFILL: No empty watering can available")
            return false
        }

        val well = Objects.stream().name("Well").nearest().first()
        if (!well.valid()) {
            script.logger.warn("REFILL: Well not valid")
            return false
        }

        script.logger.info("REFILL: Refilling watering cans")
        if (!wateringCan.interact("Use")) {
            script.logger.warn("REFILL: Unable to use watering can")
            return false
        }

        Inventory.open()
        if (!well.interact("Use")) {
            script.logger.warn("REFILL: Failed to use watering can on well")
            return false
        }

        if (!Condition.wait({ hasFullWateringCans() }, 600, 20)) {
            script.logger.warn("REFILL: Cans did not refill to 3x Watering can(8)")
            return false
        }

        TickWaiter(2).wait()
        return true
    }

    private fun unnoteBaggedPlants(): Boolean {
        val baggedPlant = Inventory.stream().name("Bagged plant 1").first()
        if (!baggedPlant.valid()) {
            script.logger.warn("REFILL: Bagged plant 1 (noted) not found for Phials")
            return false
        }

        val phials = Npcs.stream().name("Phials").nearest().first()
        if (!phials.valid()) {
            script.logger.warn("REFILL: Phials not found")
            return false
        }

        script.logger.info("REFILL: Unnoting bagged plants with Phials")
        if (!baggedPlant.interact("Use")) {
            script.logger.warn("REFILL: Unable to use bagged plant on Phials")
            return false
        }

        Inventory.open()
        if (!phials.interact("Use")) {
            script.logger.warn("REFILL: Unable to interact with Phials")
            return false
        }

        if (Condition.wait({ Chat.stream().textContains("Exchange All:").isNotEmpty() }, 600, 5)) {
            val yesOption = Chat.stream().textContains("Exchange All:").firstOrNull()
            if (yesOption != null) {
                yesOption.select()
                if (!Condition.wait({ hasUnnotedBaggedPlants() }, 300, 10)) {
                    script.logger.warn("REFILL: Did not receive unnoted bagged plants")
                    return false
                }
            } else {
                script.logger.warn("REFILL: Exchange option missing in dialog")
                return false
            }
        } else {
            script.logger.warn("REFILL: Exchange dialog never appeared")
            return false
        }

        if (Condition.wait({ Chat.canContinue() }, 600, 10)) {
            Chat.clickContinue()
        }

        TickWaiter(1).wait()

        return true
    }

    private fun reenterHouse(): Boolean {
        !Inventory.open()
        TickWaiter(1).wait()
        val portal = Objects.stream().name("Portal").action("Build mode").nearest().first()
        if (!portal.valid()) {
            script.logger.warn("REFILL: Portal to re-enter house not found")
            return false
        }

        script.logger.info("REFILL: Re-entering house for planting")
        if (!portal.interact("Build mode")) {
            script.logger.warn("REFILL: Failed to re-enter via build mode")
            return false
        }

        if (!Condition.wait({ Poh.inside() }, 300, 15)) {
            script.logger.warn("REFILL: Did not load inside POH after portal")
            return false
        }

        TickWaiter(3).wait()
        return true
    }

    private fun hasFullWateringCans(): Boolean {
        return Inventory.stream().id(FULL_WATERING_CAN_ID).count().toInt() >= REQUIRED_CAN_COUNT
    }

    private fun hasUnnotedBaggedPlants(): Boolean {
        return Inventory.stream().id(BAGGED_PLANT_ID).isNotEmpty()
    }

    private companion object {
        const val BAGGED_PLANT_ID = 8431
        const val FULL_WATERING_CAN_ID = 5340
        const val REQUIRED_CAN_COUNT = 3
    }
}
