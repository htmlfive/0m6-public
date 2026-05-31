package org.powbot.community.pestcontrol.task

import org.powbot.api.Condition
import org.powbot.api.Input
import org.powbot.api.rt4.Game
import org.powbot.api.rt4.Players
import org.powbot.community.pestcontrol.Constants
import org.powbot.community.pestcontrol.PestControl
import org.powbot.community.pestcontrol.data.PestControlMap
import org.powbot.community.pestcontrol.helpers.squire
import org.powbot.community.pestcontrol.helpers.voidKnightHealth
import kotlin.random.Random

class LeaveBoat(val script: PestControl): Task {
    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass.simpleName)
    override fun name(): String {
        return "Leaving boat"
    }

    override fun valid(): Boolean {
        return voidKnightHealth().visible() && PestControlMap.boatArea.contains(Players.local()) &&
                squire().tile().distance() > Constants.VOID_KNIGHT_NEARBY_DISTANCE
    }

    override fun run() {
        script.playedGame = true
        logger.info("Leaving boat, game starting")
        Input.tap(Game.tileToMap(squire().tile().derive(Random.nextInt(2, 4), Random.nextInt(-12, -7))))
        Condition.wait { !valid() }
    }
}
