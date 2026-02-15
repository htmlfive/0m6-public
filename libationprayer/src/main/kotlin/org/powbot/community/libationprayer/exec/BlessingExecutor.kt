package org.powbot.community.libationprayer.exec

import org.powbot.community.libationprayer.client.LibationGameClient
import org.powbot.community.libationprayer.domain.model.LibationRuntime
import org.powbot.community.libationprayer.util.LibationConstants

class BlessingExecutor(
    private val client: LibationGameClient
) {
    fun execute(runtime: LibationRuntime) {
        runtime.currentTask = "Blessing at Exposed altar"
        client.walkTo(LibationConstants.ALTAR_TILE, 6)
        if (client.blessAtExposedAltar() && client.hasBlessedWine()) {
            runtime.needsInitialFullRecharge = true
        }
    }
}

