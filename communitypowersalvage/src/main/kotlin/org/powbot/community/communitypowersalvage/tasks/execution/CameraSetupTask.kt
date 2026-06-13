package org.powbot.community.communitypowersalvage.tasks.execution

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Camera
import org.powbot.community.api.ScriptLogging
import org.powbot.community.communitypowersalvage.CommunityPowerSalvage
import org.powbot.community.communitypowersalvage.tasks.base.Task

class CameraSetupTask(script: CommunityPowerSalvage) : Task(script) {
    override fun activate(): Boolean {
        return Camera.zoom >= 10 || Camera.pitch() < 90
    }

    override fun execute() {
        script.status = "Setting up camera"
        if (Camera.zoom >= 10) {
            val targetZoom = Random.nextInt(1, 9).toDouble()
            ScriptLogging.info(script.logger, "CAMERA: Zoom is ${Camera.zoom}, setting to $targetZoom")
            Camera.moveZoomSlider(targetZoom)
            Condition.sleep(Random.nextInt(200, 400))
        }
        if (Camera.pitch() < 90) {
            val targetPitch = Random.nextInt(90, 101)
            ScriptLogging.info(script.logger, "CAMERA: Pitch is ${Camera.pitch()}, setting to $targetPitch")
            Camera.pitch(targetPitch)
            Condition.sleep(Random.nextInt(200, 400))
        }
    }
}
