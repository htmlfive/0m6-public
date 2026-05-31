package org.powbot.community.pestcontrol.data

import org.powbot.api.Tile
import org.powbot.api.rt4.Players
import org.powbot.community.pestcontrol.helpers.portalHasShield
import org.powbot.community.pestcontrol.helpers.portalHealth

enum class Portal(val xOffset: Int, val yOffset: Int, val componentIdx: Int) {
    West(-24, -15, 0), East(24, -18, 1),
    SouthEast(16, -34, 2), SouthWest(-9, -35, 3);

    fun tile(): Tile {
        return PestControlMap.squireTile.tile().derive(xOffset, yOffset)
    }

    fun health(): Int {
        return portalHealth(21 + componentIdx)
    }

    fun hasShield(): Boolean {
        return portalHasShield(25 + (componentIdx * 2))
    }

    fun gate(): Gate {
        return Gate.entries.first { it.portals.contains(this) }
    }

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger("Portal")

        fun openPortals(): List<Portal> {
            val portals = entries.filter { !it.hasShield() && it.health() > 20 }
            logger.info("Open portals found: ${portals.map { it.name }}")
            return portals
        }

        @Suppress("unused")
        fun nearestOpenPortal(): Portal? {
            return openPortals().minByOrNull { it.tile().distanceTo(Players.local().tile()) }
        }
    }
}
