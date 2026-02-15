package org.powbot.om6.mortmyre.config

import org.powbot.api.Tile
import org.powbot.om6.api.feroxpool.FeroxPoolConstants

object Constants {
    const val STOP_AT_OPTION = "Stop At Fungus"
    const val FUNGUS_NAME = "Mort myre fungus"
    const val SICKLE_NAME = "Silver sickle (b)"
    const val BLOOM_ACTION = "Cast Bloom"
    const val BLOOM_OBJECT_NAME = "Fungi on log"
    const val PICK_ACTION = "Pick"
    const val POOL_NAME = FeroxPoolConstants.POOL_OBJECT_NAME
    const val DRINK_ACTION = FeroxPoolConstants.DRINK_ACTION
    const val RING_NAME_CONTAINS = "Ring of dueling"
    const val PREFERRED_RING_NAME = "Ring of dueling(8)"
    const val WEAR_ACTION = "Wear"
    const val EQUIP_ACTION = "Equip"
    const val WIELD_ACTION = "Wield"

    const val MORT_MYRE_FUNGUS_ID = 2970
    const val BLOOM_HOP_RADIUS = 5

    val BLOOM_TILE = Tile(3474, 3419, 0)
    val POOL_TILE: Tile = FeroxPoolConstants.POOL_TILE
}
