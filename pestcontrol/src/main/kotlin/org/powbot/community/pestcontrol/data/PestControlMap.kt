package org.powbot.community.pestcontrol.data

import org.powbot.api.Area
import org.powbot.api.Tile

object PestControlMap {
    private val logger = org.slf4j.LoggerFactory.getLogger("PestControlMap")

    var voidKnightTile: Tile = Tile.Nil
    var squireTile: Tile = Tile.Nil

    var innerArea: Area = Area(Tile.Nil, Tile.Nil)
    var boatArea: Area = Area(Tile.Nil, Tile.Nil)

    fun update(squire: Tile) {
        val x = squire.x()
        val y = squire.y()

        squireTile = squire
        voidKnightTile = Tile(x + 2, y - 14)
        logger.info("Map updated - Squire: $squire, Void Knight: $voidKnightTile")

        boatArea = Area(
            Tile(x, y + 7),
            Tile(x + 4, y + 1)
        )
        innerArea = Area(
            Tile(x + 16, y - 22),
            Tile(x - 13, y + 10)
        )
    }
}
