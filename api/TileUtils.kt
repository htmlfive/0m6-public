package org.powbot.om6.api

import org.powbot.api.Tile
import kotlin.math.abs
import kotlin.math.max

/**
 * Common tile and coordinate utility functions.
 */
object TileUtils {

    private const val CHUNK_SIZE = 8

    /**
     * Parses a coordinate string in the format "X,Y,Z" into a Tile object.
     *
     * @param locationString The coordinate string to parse
     * @return A valid Tile object, or Tile.Nil if parsing fails
     */
    fun parseTileFromString(locationString: String): Tile {
        val coordString = locationString.trim()

        return try {
            val parts = coordString.split(",").map { it.trim() }

            if (parts.size == 3) {
                val x = parts[0].toInt()
                val y = parts[1].toInt()
                val z = parts[2].toInt()
                Tile(x, y, z)
            } else {
                Tile.Nil
            }
        } catch (e: Exception) {
            Tile.Nil
        }
    }

    /**
     * Formats a tile into a readable string.
     *
     * @param tile The tile to format
     * @return Formatted string representation of the tile (e.g., "(3200, 3400, 0)")
     */
    fun formatTile(tile: Tile): String {
        return "(${tile.x}, ${tile.y}, ${tile.floor})"
    }

    /**
     * Validates that a tile is not Tile.Nil and has reasonable coordinates.
     *
     * @param tile The tile to validate
     * @return true if the tile is valid, false otherwise
     */
    fun isValidTile(tile: Tile): Boolean {
        if (tile == Tile.Nil) return false

        return tile.x in 1..25000 &&
                tile.y in 1..25000 &&
                tile.floor in 0..3
    }

    /**
     * Checks if a tile is walkable by checking if it's a valid matrix tile.
     *
     * @param tile The tile to check
     * @return true if the tile appears walkable, false otherwise
     */
    fun isTileWalkable(tile: Tile): Boolean {
        return try {
            tile.matrix().valid()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Searches for a valid walkable tile within a specified range of the target tile.
     * Uses a spiral search pattern starting from the center.
     *
     * @param centerTile The target tile to search around
     * @param range The search range (searches range*2+1 tiles in each direction)
     * @return A walkable tile if found, or the original tile if none found
     */
    fun findNearestWalkableTile(centerTile: Tile, range: Int): Tile {
        // First check if the center tile itself is walkable
        if (isTileWalkable(centerTile)) {
            return centerTile
        }

        // Generate tiles in spiral order
        for (distance in 1..range) {
            for (dx in -distance..distance) {
                for (dy in -distance..distance) {
                    // Only check tiles at the current distance (edge of square)
                    if (Math.abs(dx) == distance || Math.abs(dy) == distance) {
                        val checkTile = Tile(
                            centerTile.x + dx,
                            centerTile.y + dy,
                            centerTile.floor
                        )

                        if (isValidTile(checkTile) && isTileWalkable(checkTile)) {
                            return checkTile
                        }
                    }
                }
            }
        }

        return centerTile
    }

    /**
     * Rotates a tile around its chunk based on the given rotation value.
     * Used for handling instanced regions with different orientations.
     *
     * @param tile The tile to rotate
     * @param rotation The rotation value (0-3, where 0 is no rotation)
     * @return The rotated tile, or the original tile if rotation is 0
     */
    fun rotateTile(tile: Tile, rotation: Int): Tile {
        val chunkX = tile.x and (CHUNK_SIZE - 1).inv()
        val chunkY = tile.y and (CHUNK_SIZE - 1).inv()
        val x = tile.x and (CHUNK_SIZE - 1)
        val y = tile.y and (CHUNK_SIZE - 1)

        return when (rotation) {
            1 -> Tile(chunkX + y, chunkY + (CHUNK_SIZE - 1 - x), tile.floor)
            2 -> Tile(chunkX + (CHUNK_SIZE - 1 - x), chunkY + (CHUNK_SIZE - 1 - y), tile.floor)
            3 -> Tile(chunkX + (CHUNK_SIZE - 1 - y), chunkY + x, tile.floor)
            else -> tile
        }
    }

    /**
     * Calculates the Chebyshev distance (maximum of dx and dy) between two tiles.
     * This matches the distance calculation used in the game.
     *
     * @param tile The first tile
     * @param other The second tile
     * @return The distance between the tiles (ignoring plane)
     */
    fun distanceTo(tile: Tile, other: Tile): Int {
        return max(abs(tile.x - other.x), abs(tile.y - other.y))
    }

    /**
     * Calculates the 2D Euclidean distance between two tiles (ignoring plane).
     *
     * @param tile The first tile
     * @param other The second tile
     * @return The 2D distance between the tiles
     */
    fun distanceTo2D(tile: Tile, other: Tile): Int {
        val dx = tile.x - other.x
        val dy = tile.y - other.y
        return kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toInt()
    }

    /**
     * Gets all tiles in a square range around a center tile.
     *
     * @param center The center tile
     * @param range The range in tiles (e.g., range=1 gives a 3x3 area)
     * @param localOnly If true, only returns tiles that are on the local scene (loaded)
     * @return List of tiles within the specified range
     */
    fun getTilesInRange(center: Tile, range: Int, localOnly: Boolean = false): List<Tile> {
        val tiles = mutableListOf<Tile>()

        for (dx in -range..range) {
            for (dy in -range..range) {
                val tile = Tile(center.x + dx, center.y + dy, center.floor)
                
                if (localOnly) {
                    // Only include tiles that have a valid matrix (are loaded locally)
                    if (tile.matrix().valid()) {
                        tiles.add(tile)
                    }
                } else {
                    tiles.add(tile)
                }
            }
        }

        return tiles
    }

    /**
     * Gets all local tiles (currently loaded on scene) in a range around a center tile.
     *
     * @param center The center tile
     * @param range The range in tiles
     * @return List of loaded tiles within the specified range
     */
    fun getLocalTilesInRange(center: Tile, range: Int): List<Tile> {
        return getTilesInRange(center, range, localOnly = true)
    }
}
