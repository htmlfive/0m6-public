package org.powbot.community.api

import org.powbot.api.Condition
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.World
import org.powbot.api.rt4.Worlds
import kotlin.random.Random

/**
 * Common world hopping utility functions.
 */
object WorldHopUtils {

    /**
     * Checks if there are other players on the local player's tile.
     *
     * @return True if any other player is standing on the same tile
     */
    fun isPlayerOnMyTile(): Boolean {
        val localPlayer = Players.local()
        val playersOnTile = Players.stream()
            .at(localPlayer.tile())
            .filtered { it != localPlayer }
            .count()
        return playersOnTile > 0
    }

    /**
     * Finds a random suitable world for hopping with customizable criteria.
     *
     * @param worldType The type of world to search for (default: MEMBERS)
     * @param minPopulation Minimum population (default: 0)
     * @param maxPopulation Maximum population (default: 1000)
     * @param specialty World specialty filter (default: NONE)
     * @return A random world matching the criteria, or null if none found
     */
    fun findRandomWorld(
        worldType: World.Type = World.Type.MEMBERS,
        minPopulation: Int = 0,
        maxPopulation: Int = 1000,
        specialty: World.Specialty = World.Specialty.NONE,
        servers: Set<World.Server>? = null
    ): World? {
        val suitableWorlds = Worlds.stream()
            .filtered {
                it.type() == worldType &&
                        it.population in minPopulation..maxPopulation &&
                        it.specialty() == specialty &&
                        (servers == null || servers.contains(it.server()))
            }
            .toList()

        return suitableWorlds.randomOrNull()
    }

    /**
     * Hops to a different world with a wait condition.
     *
     * @param world The world to hop to
     * @param hopTimeout Timeout for hop in milliseconds (default: 5000)
     * @param hopAttempts Max attempts for hop (default: 10)
     * @return True if the hop was successful
     */
    fun hopToWorld(world: World, hopTimeout: Int = 5000, hopAttempts: Int = 10): Boolean {
        return if (world.hop()) {
            Condition.wait(
                { !Players.local().inMotion() },
                hopTimeout,
                hopAttempts
            )
        } else {
            false
        }
    }

    /**
     * Hops to a random world matching criteria.
     *
     * @param worldType The type of world to search for (default: MEMBERS)
     * @param minPopulation Minimum population (default: 0)
     * @param maxPopulation Maximum population (default: 1000)
     * @param specialty World specialty filter (default: NONE)
     * @return True if hop was successful
     */
    fun hopToRandomWorld(
        worldType: World.Type = World.Type.MEMBERS,
        minPopulation: Int = 0,
        maxPopulation: Int = 1000,
        specialty: World.Specialty = World.Specialty.NONE,
        servers: Set<World.Server>? = null
    ): Boolean {
        val world = findRandomWorld(worldType, minPopulation, maxPopulation, specialty, servers)
        return if (world != null) {
            hopToWorld(world)
        } else {
            false
        }
    }

    /**
     * Finds the next available world by world number, starting strictly after the current world.
     * Wraps to the first matching world if none are above current.
     */
    fun findNextAvailableWorld(
        worldType: World.Type = World.Type.MEMBERS,
        specialty: World.Specialty = World.Specialty.NONE,
        servers: Set<World.Server>? = null
    ): World? {
        val current = Worlds.current().number
        val candidates = Worlds.stream()
            .filtered {
                it.type() == worldType &&
                        it.specialty() == specialty &&
                        (servers == null || servers.contains(it.server()))
            }
            .toList()
            .sortedBy { it.number }

        if (candidates.isEmpty()) return null

        return candidates.firstOrNull { it.number > current } ?: candidates.firstOrNull()
    }

    /**
     * Hops to the next available world by number, constrained by world type/specialty/server.
     */
    fun hopToNextAvailableWorld(
        worldType: World.Type = World.Type.MEMBERS,
        specialty: World.Specialty = World.Specialty.NONE,
        servers: Set<World.Server>? = null,
        hopCheckDelayMs: Int = 400,
        hopCheckAttempts: Int = 30
    ): Boolean {
        val current = Worlds.current().number
        val target = findNextAvailableWorld(worldType, specialty, servers) ?: return false
        if (target.number == current) return false

        Worlds.open()
        if (!target.hop()) return false

        return Condition.wait(
            { Worlds.current().number != current && Worlds.current().number > 0 },
            hopCheckDelayMs,
            hopCheckAttempts
        )
    }
}

