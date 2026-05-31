package org.powbot.community.pestcontrol.task

import org.powbot.api.rt4.Camera
import org.powbot.community.pestcontrol.Constants

class SetZoom: Task {
    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass.simpleName)
    override fun valid(): Boolean {
        return Camera.zoom > Constants.MAX_ZOOM_LEVEL
    }

    override fun run() {
        logger.info("Setting camera zoom to minimum")
        Camera.moveZoomSlider(0.0)
    }

    override fun name(): String {
        return "Setting zoom"
    }
}
