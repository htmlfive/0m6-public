package org.powbot.community.pestcontrol

import org.powbot.api.Condition
import org.powbot.api.rt4.Npc
import org.powbot.api.rt4.Players

object ScriptUtils {

    fun isPlayerIdle(): Boolean {
        return !Players.local().interacting().valid() && Players.local().animation() == Constants.IDLE_ANIMATION
    }

    fun attackNpc(npc: Npc): Boolean {
        return npc.interact(Constants.ATTACK_ACTION) &&
               Condition.wait { Players.local().interacting() == npc }
    }

    fun isInCombat(): Boolean {
        return Players.local().animation() != Constants.IDLE_ANIMATION ||
               Players.local().inMotion()
    }
}
