package org.powbot.community.libationprayer.exec

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Prayer
import org.powbot.community.libationprayer.client.LibationGameClient
import org.powbot.community.libationprayer.domain.model.LibationRuntime
import org.powbot.community.libationprayer.util.LibationConstants

class SacrificeExecutor(
    private val client: LibationGameClient
) {
    fun execute(runtime: LibationRuntime) {
        runtime.currentTask = "Sacrificing at Libation bowl"
        client.walkTo(LibationConstants.LIBATION_TILE, 5)
        if (client.hasBlessedWine() || client.libationBowlHasAction("Sacrifice")) {
            runtime.awaitingBowlFill = true
        }
        if (client.libationBowlHasAction("Fill")) {
            if (client.hasBlessedWine()) {
                client.clickLibationBowl()
                Condition.sleep(Random.nextInt(90, 151))
            } else {
                runtime.awaitingBowlFill = false
                return
            }
        }
        if (Prayer.prayerPoints() < 2) return
        repeat(50) {
            if (client.libationBowlHasAction("Fill")) {
                if (client.hasBlessedWine()) {
                    client.clickLibationBowl()
                    Condition.sleep(Random.nextInt(90, 151))
                    return@repeat
                }
                runtime.awaitingBowlFill = false
                return
            }
            if (Prayer.prayerPoints() < 2) {
                return
            }
            if (!client.hasBlessedWine() && !client.libationBowlHasAction("Sacrifice")) {
                return
            }
            client.clickLibationBowl()
            Condition.sleep(Random.nextInt(90, 151))
        }
    }
}

