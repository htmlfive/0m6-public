package org.powbot.community.communitypowersalvage.app

import org.powbot.api.BoundingModel
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Sailing
import org.powbot.community.communitypowersalvage.CommunityPowerSalvage
import org.powbot.community.communitypowersalvage.config.Constants

class WireframeRenderer(private val script: CommunityPowerSalvage) {
    private var lastRefresh: Long = 0L
    private val cachedModels = mutableListOf<BoundingModel>()

    fun onRender(now: Long) {
        if (!script.drawWireFrames) {
            if (cachedModels.isNotEmpty()) cachedModels.clear()
            return
        }
        if (now - lastRefresh >= 3000L) {
            lastRefresh = now
            cachedModels.clear()
            if (!Sailing.onBoat()) return
            val angle = Sailing.angle()
            if (!angle.valid()) return
            val b = Constants.Bounds.forAngle(angle.angle)
            Objects.stream().id(*Constants.ACTIVE_SHIPWRECK_IDS).within(10).inMyWorldView().forEach {
                it.boundingModel()?.let { m -> cachedModels.add(m) }
            }
            Objects.stream(10).action("Deploy").name(script.salvagingHookName).inMyWorldView().forEach {
                it.bounds(b.hX1, b.hX2, b.hY1, b.hY2, b.hZ1, b.hZ2)
                it.boundingModel()?.let { m -> cachedModels.add(m) }
            }
        }
        cachedModels.forEach { it.drawWireFrame() }
    }
}
