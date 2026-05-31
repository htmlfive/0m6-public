package org.powbot.community.pestcontrol.task

import org.powbot.api.Condition
import org.powbot.community.pestcontrol.PestControl
import org.powbot.community.pestcontrol.helpers.currentPoints
import org.powbot.community.pestcontrol.helpers.voidKnightHealth

class BoatWait(val script: PestControl) : Task {
    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass.simpleName)
    override fun name(): String {
        return "Waiting for next game"
    }

    override fun valid(): Boolean {
        return !voidKnightHealth().visible() && currentPoints().visible()
    }

    override fun run() {
        if (script.playedGame) {
            script.gamesPlayed++
            script.gamesSinceChangedActivity++
            logger.info("Game completed. Total games: ${script.gamesPlayed}")
            script.playedGame = false
        }

        val currentPointsTxt = currentPoints().text()
        if (currentPointsTxt.startsWith("Pest Points:")) {
            val currPoints = currentPointsTxt.replace("Pest Points: ", "").trim().toInt()
            if (script.initialPoints == null) {
                script.initialPoints = currPoints
                logger.info("Initial points: $currPoints")
            }

            val pointsGained = currPoints - script.initialPoints!!
            if (pointsGained > script.pointsGained) {
                logger.info("Points gained: $pointsGained (Current: $currPoints)")
                script.pointsGained = pointsGained
            }
        }

        Condition.wait {
            !valid()
        }
    }
}
