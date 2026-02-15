package org.powbot.om6.libationprayer.exec

import org.powbot.om6.libationprayer.client.LibationGameClient
import org.powbot.om6.libationprayer.domain.model.LibationRuntime
import org.powbot.om6.libationprayer.util.LibationConstants

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
