package org.powbot.community.pestcontrol.task

import org.powbot.api.rt4.walking.local.LocalPathFinder
import org.powbot.community.pestcontrol.Constants
import org.powbot.community.pestcontrol.ScriptUtils
import org.powbot.community.pestcontrol.data.Activity
import org.powbot.community.pestcontrol.helpers.nextMonster
import org.powbot.community.pestcontrol.helpers.voidKnight
import org.powbot.community.pestcontrol.helpers.voidKnightHealth


class DefendKnight(val activity: Activity): Task {

    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass.simpleName)

    override fun name(): String {
        return "Defending Knight"
    }

    override fun valid(): Boolean {
        return activity == Activity.DefendKnight && voidKnightHealth().visible()
    }

    override fun run() {
        val monster = nextMonster(voidKnight())
        if (!monster.valid()) {
            logger.info("No monsters near Void Knight")
            return
        }

        if (ScriptUtils.attackNpc(monster)) {
            logger.info("Attacking monster near Void Knight: ${monster.name()}")
            return
        } else if (monster.tile().distance() > Constants.KNIGHT_DISTANCE_THRESHOLD) {
            logger.info("Walking to monster: ${monster.name()}, distance: ${monster.tile().distance()}")
            LocalPathFinder.findPath(monster.tile()).traverse()
            return
        }
    }
}
