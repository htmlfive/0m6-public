package org.powbot.community.pestcontrol.data

import org.powbot.api.Tile

enum class Boat(val gangplankTile: Tile, val pointsPerGame: Int) {
    Easy(Tile(2658, 2639, 0), 3),
    Medium(Tile(2643, 2644, 0), 4),
    Hard(Tile(2637, 2653, 0), 5)
}
