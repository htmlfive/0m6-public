package org.powbot.om6.baggedplants.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Widgets
import org.powbot.api.waiter.TickWaiter
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.baggedplants.BaggedPlants
import org.powbot.om6.api.WidgetUtils.clickWidget

class BuildPlantTask(script: BaggedPlants) : BaggedPlants.Task(script) {

    override fun activate(): Boolean {
        return Objects.stream().name("Small Plant space 1").action("Build").first().valid()
    }

    override fun execute() {
        val space = Objects.stream().name("Small Plant space 1").action("Build").nearest().first()

        if (!space.valid()) {
            script.logger.warn("BUILD: Plant space not found")
            return
        }
        if (Inventory.stream().id(8431).isNotEmpty()) {
            ensureYawInRange()
            script.logger.info("BUILD: Building plant")
            if (space.interact("Build")) {
                script.logger.info("BUILD: Clicking build option")
                Condition.wait({
                    Widgets.widget(458).component(4).component(6).visible()
                }, 100, 50)
                if (clickWidget(458, 4, 6, "Build")) {
                    script.plantsBuilt++
                    script.logger.info("BUILD: Plant built successfully (Total: ${script.plantsBuilt})")
                    Condition.wait({
                        !Widgets.widget(458).component(4).component(6).visible()
                    }, 100, 50)
                    TickWaiter(2).wait()
                }
            }
        } else {
            ScriptManager.pause()
        }
    }

    private fun ensureYawInRange() {
        val yaw = Camera.yaw()
        if (yaw in MIN_YAW..MAX_YAW) {
            return
        }
        val target = Random.nextInt(MIN_YAW, MAX_YAW + 1)
        if (Camera.yaw() == target) {
            Condition.wait({ Camera.yaw() in MIN_YAW..MAX_YAW }, 100, 10)
        }
    }

    private companion object {
        const val MIN_YAW = 85
        const val MAX_YAW = 93
    }
}
