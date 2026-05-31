package org.powbot.community.pestcontrol.helpers

import org.powbot.api.Condition
import org.powbot.api.Input
import org.powbot.api.rt4.Game
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Players
import org.powbot.community.pestcontrol.Constants
import org.powbot.community.pestcontrol.data.Portal

private val logger = org.slf4j.LoggerFactory.getLogger("Movement")

fun Movement.walkToPortal(portal: Portal): Boolean {
    val player = Players.local()

    if (player.tile().distanceTo(portal.tile()) < Constants.PORTAL_TILE_PROXIMITY) {
        return true
    }

    if (!Movement.reachable(Players.local().tile(), portal.tile())) {
        val gate = Objects.stream(portal.gate().tile(), Constants.GATE_SEARCH_DISTANCE)
            .name(Constants.GATE_NAME)
            .action(Constants.OPEN_ACTION).firstOrNull {
                !it.actions().contains(Constants.REPAIR_ACTION)
            }

        if (gate != null) {
            logger.info("Opening gate: ${portal.gate().name} for portal: ${portal.name}")
            if (gate.interact("Open")) {
                return Condition.wait {
                    !gate.valid()
                }
            }

            Input.tap(Game.tileToMap(Movement.closestOnMap(gate.tile())))
            return false
        }
    }

    Input.tap(Game.tileToMap(Movement.closestOnMap(portal.tile())))
    return false
}
