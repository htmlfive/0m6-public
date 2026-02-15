package org.powbot.om6.baggedplants.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.Chat
import org.powbot.api.rt4.Objects
import org.powbot.api.waiter.TickWaiter
import org.powbot.om6.baggedplants.BaggedPlants

class RemovePlantTask(script: BaggedPlants) : BaggedPlants.Task(script) {

    override fun activate(): Boolean {
        return Objects.stream().name("Plant").action("Remove").first().valid()
    }

    override fun execute() {
        val plant = Objects.stream().name("Plant").action("Remove").nearest().first()
        
        if (!plant.valid()) {
            script.logger.warn("REMOVE: Plant not found")
            return
        }
        ensureYawInRange()
        TickWaiter(1).wait()

        script.logger.info("REMOVE: Removing plant")
        if (plant.interact("Remove")) {

            if (Condition.wait({ Chat.stream().textContains("Yes.").isNotEmpty() }, 600, 5)) {
                val yesOption = Chat.stream().textContains("Yes.").firstOrNull()
                if (yesOption != null) {
                    script.logger.info("REMOVE: Selecting Yes option")
                    yesOption.select()
                    TickWaiter(2).wait()

                }
            }
        }
    }

    private fun ensureYawInRange() {
        val yaw = Camera.yaw()
        if (yaw in MIN_YAW..MAX_YAW) {
            return
        }
        val target = Random.nextInt(MIN_YAW, MAX_YAW + 1)
        if (Camera.angle(target)) {
            Condition.wait({ Camera.yaw() in MIN_YAW..MAX_YAW }, 100, 10)
        }
    }

    private companion object {
        const val MIN_YAW = 85
        const val MAX_YAW = 93
    }
}
