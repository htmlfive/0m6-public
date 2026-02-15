package org.powbot.community.api.feroxpool

import org.powbot.api.Area
import org.powbot.api.Tile

object FeroxPoolConstants {
    const val POOL_OBJECT_NAME = "Pool of refreshment"
    const val POOL_OBJECT_ID = 39651
    const val DRINK_ACTION = "Drink"

    val POOL_TILE: Tile = Tile(3129, 3635, 0)
    val BANK_TILE: Tile = Tile(3135, 3631, 0)
    val POOL_AREA: Area = Area(Tile(3128, 3637, 0), Tile(3130, 3634, 0))
}

