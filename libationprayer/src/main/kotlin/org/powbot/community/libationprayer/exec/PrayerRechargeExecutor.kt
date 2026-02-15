package org.powbot.community.libationprayer.exec

import org.powbot.api.rt4.Prayer
import org.powbot.api.rt4.Skills
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.community.libationprayer.client.LibationGameClient
import org.powbot.community.libationprayer.domain.model.LibationRuntime
import org.powbot.community.libationprayer.util.LibationConstants

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

