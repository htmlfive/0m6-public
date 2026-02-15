package org.powbot.community.api

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.Widgets
import org.powbot.api.script.AbstractScript

object CameraUtils {
    private const val COMPASS_WIDGET_ID = 601
    private const val COMPASS_COMPONENT_INDEX = 35
    private const val PITCH_MIN = 35
    private const val PITCH_MAX = 41

    enum class CardinalDirection(val yaw: Int, val action: String) {
        North(0, "Look North"),
        South(180, "Look South"),
        East(270, "Look East"),
        West(90, "Look West")
    }

    fun snapCameraToDirection(direction: CardinalDirection, script: AbstractScript) {
        val yawNeeded = direction.yaw
        val compassAction = direction.action
        script.logger.info("LOGIC: Attempting to snap camera to ${direction.name} (Yaw: $yawNeeded). Current Yaw: ${Camera.yaw()}, Current Pitch: ${Camera.pitch()}")

        val isPitchIncorrect = Camera.pitch() !in PITCH_MIN..PITCH_MAX

        if (Camera.yaw() != yawNeeded || isPitchIncorrect) {
            script.logger.info("CHECK: Yaw is ${Camera.yaw()}, does not match target $compassAction ($yawNeeded).")

            val compassComponent = Widgets.widget(COMPASS_WIDGET_ID).component(COMPASS_COMPONENT_INDEX)
            script.logger.info("WIDGET: Checking compass component $COMPASS_WIDGET_ID:$COMPASS_COMPONENT_INDEX (Valid: ${compassComponent.valid()}).")

            if (compassComponent.valid() && compassComponent.click(compassAction)) {
                script.logger.info("ACTION: Successfully clicked compass with action '$compassAction'. Waiting for yaw stabilization.")
                val snapSuccess = Condition.wait({ Camera.yaw() == yawNeeded }, 100, 10)
                if (snapSuccess) {
                    script.logger.info("SUCCESS: Camera yaw stabilized at $yawNeeded.")
                } else {
                    script.logger.warn("FAIL: Camera yaw failed to stabilize at $yawNeeded after clicking compass.")
                }
                val sleepTime = Random.nextInt(600, 1200)
                script.logger.info("SLEEP: Sleeping for $sleepTime ms after snap attempt.")
                Condition.sleep(sleepTime)
            } else {
                script.logger.warn("FAIL: Failed to find or click compass component $COMPASS_WIDGET_ID:$COMPASS_COMPONENT_INDEX for action '$compassAction'.")
            }
        } else {
            script.logger.info("CHECK: Already facing $compassAction ($yawNeeded). No snap action needed.")
        }
    }
}

