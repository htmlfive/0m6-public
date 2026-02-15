package org.powbot.community.api

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Players
import kotlin.random.Random

/**
 * Common movement and positioning utility functions.
 */
object MovementUtils {

    /**
     * Checks if the player is at a specific tile.
     *
     * @param tile The tile to check
     * @return True if the player is at the specified tile
     */
    fun isAtTile(tile: Tile, tolerance: Int = 0): Boolean {
        val playerTile = Players.local().tile()
        return if (tolerance <= 0) {
            playerTile == tile
        } else {
            playerTile.distanceTo(tile) <= tolerance
        }
    }

    /**
     * Gets the distance to a tile from the local player.
     *
     * @param tile The tile to measure distance to
     * @return The distance in tiles
     */
    fun distanceToTile(tile: Tile): Double {
        return tile.distance().toDouble()
    }

    /**
     * Walks to a tile if not already there with interaction timeout and fallback.
     *
     * @param targetTile The tile to walk to
     * @param fallbackTile Optional stall/fallback tile for finding nearby tiles
     * @param interactionTimeout Time in ms to wait before finding fallback tile (default 10000ms)
     * @param maxRetries Maximum number of retries (default: 3)
     * @return True if reached target or fallback tile
     */
    fun walkToTile(
        targetTile: Tile,
        fallbackTile: Tile? = null,
        interactionTimeout: Long = 10000,
        maxRetries: Int = 3
    ): Boolean {
        if (isAtTile(targetTile)) {
            return true
        }

        val startTime = System.currentTimeMillis()
        var lastInteractionTime = startTime
        var currentTarget = targetTile
        var fallbackAttempted = false
        var retryCount = 0

        val walked = Condition.wait({
            val elapsedTime = System.currentTimeMillis() - startTime
            val timeSinceInteraction = System.currentTimeMillis() - lastInteractionTime

            if (elapsedTime >= 30000) {
                if (retryCount < maxRetries && fallbackTile != null) {
                    val newFallback = findNearbyValidTile(fallbackTile)
                    if (newFallback != null && newFallback != currentTarget) {
                        currentTarget = newFallback
                        retryCount++
                        lastInteractionTime = System.currentTimeMillis()
                        return@wait false
                    }
                }
                return@wait true
            }

            if (isAtTile(currentTarget)) {
                if (currentTarget != targetTile) {
                    Movement.step(targetTile)
                    Condition.sleep(200)
                }
                return@wait true
            }

            if (timeSinceInteraction >= interactionTimeout && !fallbackAttempted && fallbackTile != null) {
                val fallback = findNearbyValidTile(fallbackTile)
                if (fallback != null) {
                    currentTarget = fallback
                    fallbackAttempted = true
                }
            }

            val distance = currentTarget.distance()

            if (distance <= 5 && currentTarget.reachable()) {
                Movement.step(currentTarget)
                lastInteractionTime = System.currentTimeMillis()
                Condition.sleep(200)
            } else {
                Movement.walkTo(currentTarget)
                lastInteractionTime = System.currentTimeMillis()
            }

            false
        }, 200, 25)

        return walked
    }

    /**
     * Finds a nearby valid tile within 1 tile of the center.
     *
     * @param centerTile The center tile to search around
     * @return A valid reachable tile or null
     */
    fun findNearbyValidTile(centerTile: Tile): Tile? {
        val nearby = listOf(
            centerTile,
            centerTile.derive(1, 0),
            centerTile.derive(-1, 0),
            centerTile.derive(0, 1),
            centerTile.derive(0, -1),
            centerTile.derive(1, 1),
            centerTile.derive(1, -1),
            centerTile.derive(-1, 1),
            centerTile.derive(-1, -1)
        )

        return nearby.find { it.reachable() }
    }

    /**
     * Enables run if needed based on energy level.
     * @param minRunEnergy Minimum energy level to enable run (default: 30)
     */
    fun enableRunIfNeeded(minRunEnergy: Int = 30) {
        if (!Movement.running() && Movement.energyLevel() > minRunEnergy) {
            Movement.running(true)
        }
    }

    /**
     * Walks directly to a tile using simple Movement.walkTo logic.
     */
    fun walkDirectToTile(targetTile: Tile, tolerance: Int = 0): Boolean {
        if (isAtTile(targetTile, tolerance)) return true
        Movement.walkTo(targetTile)
        return Condition.wait({ isAtTile(targetTile, tolerance) }, 300, 10)
    }

    /**
     * Walks to a tile with small randomization to appear less bot-like.
     */
    fun walkToTileRandomized(targetTile: Tile, tolerance: Int = 0, randomizeBy: Int = 1): Boolean {
        if (isAtTile(targetTile, tolerance)) return true

        var randomizedTile: Tile? = null
        repeat(10) {
            val offsetX = (-randomizeBy..randomizeBy).random()
            val offsetY = (-randomizeBy..randomizeBy).random()
            val candidateTile = Tile(targetTile.x + offsetX, targetTile.y + offsetY, targetTile.floor)
            if (candidateTile.valid()) {
                randomizedTile = candidateTile
                return@repeat
            }
        }

        val finalTile = randomizedTile ?: targetTile
        Movement.walkTo(finalTile)
        return Condition.wait({ isAtTile(targetTile, tolerance) }, 100, 5)
    }

    /**
     * Checks distance between local player and a tile.
     */
    fun isWithinDistance(tile: Tile, maxDistance: Int): Boolean {
        return Players.local().tile().distanceTo(tile) <= maxDistance
    }

    /**
     * Safely closes the bank if it's open.
     */
    fun closeBankIfOpen() {
        if (Bank.opened()) {
            Bank.close()
            Condition.wait({ !Bank.opened() }, 500, 5)
        }
    }

    /**
     * Checks if the player is idle (not in motion and not animating).
     *
     * @return True if the player is completely idle
     */
    fun isPlayerIdle(): Boolean {
        val player = Players.local()
        return player.animation() == -1 && !player.inMotion() && !player.interacting().valid()
    }

    /**
     * Calculates the angle between two tiles.
     * @param from The starting tile
     * @param to The ending tile
     * @return The angle in degrees
     */
    fun angleBetween(from: Tile, to: Tile): Double {
        return Math.toDegrees(kotlin.math.atan2((to.y() - from.y()).toDouble(), (to.x() - from.x()).toDouble()))
    }
}

