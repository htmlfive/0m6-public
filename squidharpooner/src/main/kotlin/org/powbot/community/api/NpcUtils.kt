package org.powbot.community.api

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.Npc
import org.powbot.api.rt4.Npcs
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.walking.local.LocalPathFinder

object NpcUtils {
    fun findNpc(name: String, maxDistance: Int = 15, maxLocalPathSteps: Int? = null): Npc {
        val player = Players.local()
        val playerTile = player.tile()
        val candidates = Npcs.stream()
            .name(name)
            .within(maxDistance)
            .nearest()
            .toList()

        val filtered = filterByLocalPath(candidates, player.valid(), playerTile, maxLocalPathSteps)
        return filtered.firstOrNull() ?: Npc.Nil
    }

    fun findNpcByNames(names: List<String>, maxDistance: Int = 15, maxLocalPathSteps: Int? = null): Npc {
        val player = Players.local()
        val playerTile = player.tile()
        val candidates = Npcs.stream()
            .filtered { candidate -> names.any { candidate.name().equals(it, ignoreCase = true) } }
            .within(maxDistance)
            .filtered { it.healthPercent() != 0 }
            .nearest()
            .toList()

        val filtered = filterByLocalPath(candidates, player.valid(), playerTile, maxLocalPathSteps)
        return filtered.firstOrNull() ?: Npc.Nil
    }

    private fun filterByLocalPath(
        npcs: List<Npc>,
        playerValid: Boolean,
        playerTile: Tile,
        maxLocalPathSteps: Int?
    ): List<Npc> {
        if (!playerValid || maxLocalPathSteps == null || npcs.isEmpty()) {
            return npcs
        }

        return npcs.filter { npc ->
            val localPath = LocalPathFinder.findPath(playerTile, npc.tile(), true)
            val steps = localPath.size
            steps in 1..maxLocalPathSteps
        }
    }

    fun interactAndWait(npc: Npc, action: String, waitCondition: () -> Boolean): Boolean {
        if (!npc.valid()) return false
        return if (npc.interact(action)) {
            Condition.wait(waitCondition, 100, 30)
        } else {
            false
        }
    }

    fun interactAndWaitForCombat(npc: Npc, action: String = "Attack"): Boolean {
        return interactAndWait(npc, action) { Players.local().interacting().valid() }
    }
}

