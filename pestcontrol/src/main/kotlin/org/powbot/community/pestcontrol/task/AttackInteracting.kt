package org.powbot.community.pestcontrol.task

import org.powbot.api.rt4.Npcs
import org.powbot.community.pestcontrol.ScriptUtils

class AttackInteracting: Task {
    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass.simpleName)
    override fun name(): String {
        return "Attacks interacting npc"
    }

    override fun valid(): Boolean {
        return Npcs.stream().interactingWithMe().isNotEmpty() && ScriptUtils.isPlayerIdle()
    }

    override fun run() {
        val npc = Npcs.stream().interactingWithMe().first()
        if (npc.valid()) {
            logger.info("Attacking interacting NPC: ${npc.name()}")
            ScriptUtils.attackNpc(npc)
        }
    }
}
