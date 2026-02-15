package org.powbot.om6.libationprayer.exec

import org.powbot.api.rt4.Prayer
import org.powbot.api.rt4.Skills
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.om6.libationprayer.client.LibationGameClient
import org.powbot.om6.libationprayer.domain.model.LibationRuntime
import org.powbot.om6.libationprayer.util.LibationConstants

class PrayerRechargeExecutor(
    private val client: LibationGameClient
) {
    fun execute(runtime: LibationRuntime) {
        runtime.currentTask = "Recharging prayer at Shrine"
        client.walkTo(LibationConstants.SHRINE_TILE, 5)
        if (client.baskAtShrine() && Prayer.prayerPoints() >= Skills.realLevel(Skill.Prayer)) {
            runtime.needsInitialFullRecharge = false
        }
    }
}
