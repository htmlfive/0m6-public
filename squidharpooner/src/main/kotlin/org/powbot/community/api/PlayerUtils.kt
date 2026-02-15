package org.powbot.community.api

import org.powbot.api.rt4.Players

object PlayerUtils {
    fun areOtherPlayersNearby(radius: Int = 10): Boolean {
        val localPlayer = Players.local()
        return Players.stream()
            .within(radius)
            .filtered { it != localPlayer }
            .count() > 0
    }

    fun isInCombat(): Boolean = Players.local().inCombat()
}

