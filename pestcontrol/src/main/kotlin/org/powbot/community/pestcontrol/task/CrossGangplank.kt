package org.powbot.community.pestcontrol.task

import org.powbot.api.Condition
import org.powbot.api.Notifications
import org.powbot.api.Random
import org.powbot.api.Random.nextInt
import org.powbot.api.rt4.Chat
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Skills
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.mobile.script.ScriptManager
import org.powbot.community.pestcontrol.Constants
import org.powbot.community.pestcontrol.PestControl
import org.powbot.community.pestcontrol.data.Boat
import org.powbot.community.pestcontrol.helpers.currentPoints
import org.powbot.community.pestcontrol.helpers.voidKnightHealth

class CrossGangplank(val boat: Boat, val script: PestControl): Task {
    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass.simpleName)
    override fun name(): String {
        return "Entering boat"
    }

    override fun valid(): Boolean {
        return !currentPoints().visible() && !voidKnightHealth().visible()
    }

    override fun run() {
        if (shouldStopScript()) {
            return
        }

        Condition.wait({
            Chat.canContinue()
        }, Constants.CHAT_WAIT_ATTEMPTS, Constants.CHAT_WAIT_INTERVAL)

        if (Chat.canContinue()) {
            logger.info("Closing chat dialog")
            if (Random.nextBoolean()) {
                Chat.clickContinue()
                Condition.wait {
                    !Chat.canContinue()
                }
                return
            } else {
                Condition.sleep(nextInt(Constants.CHAT_CLOSE_MIN_DELAY, Constants.CHAT_CLOSE_MAX_DELAY))
            }
        }

        val gangplank = Objects.stream(Constants.OBJECT_SEARCH_DISTANCE)
            .name(Constants.GANGPLANK_NAME)
            .within(boat.gangplankTile, Constants.GANGPLANK_SEARCH_DISTANCE).first()
        if (gangplank.valid()) {
            if (gangplank.interact(Constants.CROSS_ACTION)) {
                logger.info("Crossing gangplank for ${boat.name} boat")
                Condition.wait { currentPoints().visible() }
                return
            } else if (gangplank.tile.distance() > Constants.GANGPLANK_SEARCH_DISTANCE) {
                logger.info("Walking to gangplank")
                Movement.walkTo(gangplank)
                return
            }
        }
    }

    private fun shouldStopScript(): Boolean {
        if (script.stopAtAttackLevel > 0 && Skills.realLevel(Skill.Attack) >= script.stopAtAttackLevel) {
            logger.info("Reached target Attack level: ${script.stopAtAttackLevel}")
            Notifications.showNotification("Reached target Attack level: ${script.stopAtAttackLevel}")
            ScriptManager.stop()
            return true
        }
        if (script.stopAtStrengthLevel > 0 && Skills.realLevel(Skill.Strength) >= script.stopAtStrengthLevel) {
            logger.info("Reached target Strength level: ${script.stopAtStrengthLevel}")
            Notifications.showNotification("Reached target Strength level: ${script.stopAtStrengthLevel}")
            ScriptManager.stop()
            return true
        }
        if (script.stopAtDefenceLevel > 0 && Skills.realLevel(Skill.Defence) >= script.stopAtDefenceLevel) {
            logger.info("Reached target Defence level: ${script.stopAtDefenceLevel}")
            Notifications.showNotification("Reached target Defence level: ${script.stopAtDefenceLevel}")
            ScriptManager.stop()
            return true
        }
        if (script.stopAtRangedLevel > 0 && Skills.realLevel(Skill.Ranged) >= script.stopAtRangedLevel) {
            logger.info("Reached target Ranged level: ${script.stopAtRangedLevel}")
            Notifications.showNotification("Reached target Ranged level: ${script.stopAtRangedLevel}")
            ScriptManager.stop()
            return true
        }
        if (script.stopAtMagicLevel > 0 && Skills.realLevel(Skill.Magic) >= script.stopAtMagicLevel) {
            logger.info("Reached target Magic level: ${script.stopAtMagicLevel}")
            Notifications.showNotification("Reached target Magic level: ${script.stopAtMagicLevel}")
            ScriptManager.stop()
            return true
        }

        if (script.stopAtPoints > 0 && script.pointsGained >= script.stopAtPoints) {
            logger.info("Reached target points: ${script.stopAtPoints}")
            Notifications.showNotification("Reached target points: ${script.stopAtPoints}")
            ScriptManager.stop()
            return true
        }

        return false
    }
}
