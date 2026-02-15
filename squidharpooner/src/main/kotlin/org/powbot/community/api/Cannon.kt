package org.powbot.community.api

import org.powbot.api.Tile
import org.powbot.api.rt4.GameObject
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.VarpbitConstants
import org.powbot.api.rt4.Varpbits
import org.powbot.api.rt4.Worlds
import java.util.prefs.Preferences

/**
 * Helpers for interacting with the player-owned dwarf multicannon.
 */
object Cannon {
    const val BASE = 6
    const val STAND = 8
    const val BARRELS = 10
    const val FURNACE = 12
    val items = intArrayOf(BASE, STAND, BARRELS, FURNACE)

    private const val STATE = 2
    private const val BALLS = 3
    private const val COORDS = 4

    private const val KEY_TILE = "cannon_tile"
    private const val KEY_WORLD = "cannon_world"

    private val preferences: Preferences by lazy {
        Preferences.userNodeForPackage(Cannon::class.java)
    }

    val balls: Int
        get() = balls()

    val state: Int
        get() = state()

    val firing: Boolean
        get() = firing()

    val placed: Boolean
        get() = placed()

    val cannonTile: Tile
        get() = tile()

    val cannonObject: GameObject
        get() = getCannon()

    fun balls(): Int = Varpbits.varpbit(BALLS)

    fun state(): Int = Varpbits.varpbit(STATE)

    fun firing(): Boolean = Varpbits.varpbit(1) != 0

    fun placed(): Boolean = state() == 4

    fun tile(): Tile {
        val c = Varpbits.varpbit(COORDS)
        val x = (c shr 14 and 0x3FFF) + 1
        val y = (c and 0x3FFF) + 1
        val z = c shr 28 and 0x3
        return Tile(x, y, z)
    }

    fun getCannon(): GameObject =
        Objects.stream(tile(), GameObject.Type.INTERACTIVE)
            .name("Broken multicannon", "Dwarf multicannon")
            .first()


    fun rememberPlacement(tile: Tile = tile(), world: Int = currentWorldNumber()) {
        preferences.put(KEY_TILE, encodeTile(tile))
        preferences.putInt(KEY_WORLD, world)
    }

    fun rememberedTile(): Tile? =
        decodeTile(preferences.get(KEY_TILE, ""))

    fun rememberedWorld(): Int? =
        preferences.getInt(KEY_WORLD, -1).takeIf { it > 0 }

    fun clearRememberedPlacement() {
        preferences.remove(KEY_TILE)
        preferences.remove(KEY_WORLD)
    }

    fun hasRememberedPlacement(): Boolean =
        rememberedTile() != null && rememberedWorld() != null

    fun isRememberedCannonInCurrentWorld(): Boolean =
        rememberedWorld()?.let { it == currentWorldNumber() } ?: false

    fun isRememberedCannonActive(): Boolean {
        val storedTile = rememberedTile() ?: return false
        val storedWorld = rememberedWorld() ?: return false
        val currentWorld = currentWorldNumber()
        if (storedWorld != currentWorld) {
            return false
        }
        return Objects.stream(storedTile, GameObject.Type.INTERACTIVE)
            .name("Broken multicannon", "Dwarf multicannon")
            .first()
            .valid()
    }

    fun ownsPlacedCannon(): Boolean {
        val storedTile = rememberedTile() ?: return false
        val storedWorld = rememberedWorld() ?: return false
        val currentWorld = currentWorldNumber()
        if (storedWorld != currentWorld) {
            return false
        }
        if (!placed()) {
            return false
        }
        val activeTile = tile()
        return activeTile.x == storedTile.x &&
            activeTile.y == storedTile.y &&
            activeTile.floor == storedTile.floor
    }

    private fun encodeTile(tile: Tile): String =
        "${tile.x},${tile.y},${tile.floor}"

    private fun decodeTile(value: String): Tile? {
        if (value.isBlank()) {
            return null
        }
        val parts = value.split(',')
        if (parts.size != 3) {
            return null
        }
        val (xRaw, yRaw, planeRaw) = parts
        return runCatching {
            Tile(xRaw.toInt(), yRaw.toInt(), planeRaw.toInt())
        }.getOrNull()
    }

    private fun currentWorldNumber(): Int =
        runCatching { Worlds.current().number }.getOrDefault(-1)
}

