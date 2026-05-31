package org.powbot.community.pestcontrol.data

import org.powbot.api.Tile

enum class Gate(val xOffset: Int, val yOffset: Int, vararg val portals: Portal) {
    East(15, -15, Portal.East), West(-12,  -14, Portal.West),
    South(1, -22, Portal.SouthEast, Portal.SouthWest);

    fun tile(): Tile {
        return PestControlMap.squireTile.tile().derive(xOffset, yOffset)
    }
}
