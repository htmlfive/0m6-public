package org.powbot.om6.api

import com.google.common.eventbus.Subscribe
import org.powbot.api.Tile
import org.powbot.api.event.RenderEvent
import org.powbot.api.rt4.Players
import org.powbot.mobile.drawing.Rendering
import java.awt.Color
import java.awt.Graphics

/**
 * Utility functions for rendering tiles and objects on screen
 */
object RenderUtils {

    /**
     * Draws a single tile on screen if it's in viewport
     * 
     * Example usage in script:
     * ```
     * @Subscribe
     * fun onRender(r: RenderEvent) {
     *     val targetTile = Tile(3200, 3400, 0)
     *     RenderUtils.drawTile(targetTile)
     * }
     * ```
     */
    fun drawTile(tile: Tile) {
        val matrix = tile.matrix()
        if (matrix.inViewport()) {
            Rendering.drawPolygon(matrix.bounds())
        }
    }

    /**
     * Draws a single tile with custom color
     * 
     * Example usage:
     * ```
     * @Subscribe
     * fun onRender(r: RenderEvent) {
     *     RenderUtils.drawTile(r.graphics, altarTile, Color.RED)
     * }
     * ```
     */
    fun drawTile(g: Graphics, tile: Tile, color: Color) {
        val matrix = tile.matrix()
        if (matrix.inViewport()) {
            val oldColor = g.color
            g.color = color
            val bounds = matrix.bounds()
            g.drawPolygon(bounds.xpoints, bounds.ypoints, bounds.npoints)
            g.color = oldColor
        }
    }

    /**
     * Draws multiple tiles on screen
     * 
     * Example usage:
     * ```
     * @Subscribe
     * fun onRender(r: RenderEvent) {
     *     val tiles = listOf(
     *         Tile(3200, 3400, 0),
     *         Tile(3201, 3401, 0)
     *     )
     *     RenderUtils.drawTiles(tiles)
     * }
     * ```
     */
    fun drawTiles(tiles: List<Tile>) {
        tiles.forEach { tile ->
            drawTile(tile)
        }
    }

    /**
     * Draws multiple tiles with custom colors
     * 
     * Example usage:
     * ```
     * @Subscribe
     * fun onRender(r: RenderEvent) {
     *     RenderUtils.drawTiles(r.graphics, listOf(
     *         altarTile to Color.RED,
     *         boatTile to Color.BLUE,
     *         nearAltarTile to Color.GREEN
     *     ))
     * }
     * ```
     */
    fun drawTiles(g: Graphics, tilesWithColors: List<Pair<Tile, Color>>) {
        tilesWithColors.forEach { (tile, color) ->
            drawTile(g, tile, color)
        }
    }

    /**
     * Generates tiles for player radius with tail in front based on facing direction
     * Returns list of tiles: 3x3 around player + 3x3 in front of player
     */

//    @Subscribe
//    fun onRender(r: RenderEvent) {
//        drawTiles(RenderUtils.getPlayerRadiusWithTail())
//    }

    fun getPlayerRadiusWithTail(): List<Tile> {
        val player = Players.local()
        val playerTile = player.tile()
        val facing = player.orientation()
        val tiles = mutableListOf<Tile>()

        // 3x3 around player (radius 1)
        for (dx in -1..1) {
            for (dy in -1..1) {
                tiles.add(Tile(playerTile.x + dx, playerTile.y + dy, playerTile.floor))
            }
        }

        // Calculate tail direction
        val (tailX, tailY) = when (facing) {
            0 -> Pair(0, 2)   // south (negative Y)
            2 -> Pair(2, 0)   // west (positive X)
            4 -> Pair(0, -2)  // north (positive Y)
            6 -> Pair(-2, 0)  // east (negative X)
            else -> Pair(0, 0)
        }

        // 3x3 tail
        for (dx in -1..1) {
            for (dy in -1..1) {
                tiles.add(Tile(playerTile.x + tailX + dx, playerTile.y + tailY + dy, playerTile.floor))
            }
        }

        return tiles
    }
}
