package org.powbot.community.pestcontrol.task

import org.powbot.api.Condition
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Npcs
import org.powbot.api.rt4.Players
import org.powbot.community.pestcontrol.Constants
import org.powbot.community.pestcontrol.ScriptUtils
import org.powbot.community.pestcontrol.data.Activity
import org.powbot.community.pestcontrol.data.Portal
import org.powbot.community.pestcontrol.helpers.nextMonster
import org.powbot.community.pestcontrol.helpers.voidKnightHealth
import org.powbot.community.pestcontrol.helpers.walkToPortal

class AttackPortal(val activity: Activity): Task {

    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass.simpleName)
    var portal: Portal? = null

    override fun name(): String {
        return "Attacking Portal (${portal?.name ?: "-"})"
    }

    override fun valid(): Boolean {
        return activity == Activity.AttackPortal && voidKnightHealth().visible() && ScriptUtils.isPlayerIdle()
    }

    override fun run() {
        if (portal == null || portal?.health() == 0) {
            portal = Portal.openPortals().randomOrNull() ?:
                    Portal.entries.random()
            logger.info("Selected new portal: ${portal?.name}")
            return
        }

        val portalDistance = portal!!.tile().distance()
        if (portalDistance <= Constants.PORTAL_ATTACK_DISTANCE) {
            if (portal!!.health() > 0 && !portal!!.hasShield() && attackPortal()) {
                logger.info("Attacking portal: ${portal!!.name}, health: ${portal!!.health()}")
                return
            }
        }

        if (fightBrawler()) {
            logger.info("Fighting nearby Brawler")
            return
        }
        if (fightSpinner()) {
            logger.info("Fighting Spinner (3+ nearby)")
            return
        }

        if (portal!!.tile().distance() <= Constants.PORTAL_ATTACK_DISTANCE) {
            val monster = nextMonster(portal!!.tile())
            if (monster.valid()) {
                logger.info("Attacking monster near portal: ${monster.name()}")
                ScriptUtils.attackNpc(monster)
                return
            } else if (portal?.health() == 0) {
                logger.info("Portal destroyed: ${portal?.name}")
                portal = null
            }
            return
        }

        if (portal!!.tile().distance() > Constants.PORTAL_ATTACK_DISTANCE) {
            if ((portal?.health() ?: 0) < Constants.PORTAL_LOW_HEALTH_THRESHOLD) {
                logger.info("Portal low health, selecting new target")
                portal = null
                return
            }

            logger.info("Walking to portal: ${portal!!.name}, distance: $portalDistance")
            Movement.walkToPortal(portal!!)
            return
        }
        return
    }

    private fun fightBrawler(): Boolean {
        val brawlers = Npcs.stream().within(Constants.BRAWLER_PRIORITY_DISTANCE.toDouble())
            .name(Constants.BRAWLER_NAME).nearest().list()
        if (brawlers.isEmpty()) {
            return false
        }

        val brawler = brawlers.random()
        return brawler.valid() && ScriptUtils.attackNpc(brawler)
    }

    private fun fightSpinner(): Boolean {
        val spinners = Npcs.stream()
            .within(Constants.SPINNER_SEARCH_DISTANCE.toDouble())
            .name(Constants.SPINNER_NAME)
            .nearest()
            .filtered { it.healthPercent() >= Constants.SPINNER_HEALTH_THRESHOLD }
            .toList()
        if (spinners.size <= Constants.SPINNER_MIN_COUNT) {
            return false
        }

        val spinner = spinners.random()
        return spinner.valid() && ScriptUtils.attackNpc(spinner)
    }

    private fun attackPortal(): Boolean {
        val portal = Npcs.stream().name(Constants.PORTAL_NAME)
            .within(Constants.PORTAL_SEARCH_DISTANCE).nearest().first()
        if (!portal.valid() || !portal.actions().contains(Constants.ATTACK_ACTION)) {
            return false
        }

        return portal.interact(Constants.ATTACK_ACTION) &&
                Condition.wait { Players.local().animation() != Constants.IDLE_ANIMATION && !Players.local().inMotion() }
    }
}
