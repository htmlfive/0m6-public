package org.powbot.community.api

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import kotlin.random.Random

/**
 * Common game object interaction utilities.
 */
object GameObjectUtils {

    /**
     * Finds a game object by ID within a certain range of a tile.
     *
     * @param objectId The ID of the object to find
     * @param centerTile The tile to search around
     * @param range The maximum distance from the center tile
     * @return The found GameObject, or GameObject.Nil if not found
     */
    fun findGameObject(objectId: Int, centerTile: Tile, range: Double): GameObject {
        return Objects.stream()
            .id(objectId)
            .within(centerTile, range)
            .nearest()
            .firstOrNull() ?: GameObject.Nil
    }

    /**
     * Finds a game object by name within a certain range of a tile.
     *
     * @param objectName The name of the object to find
     * @param centerTile The tile to search around
     * @param range The maximum distance from the center tile
     * @return The found GameObject, or GameObject.Nil if not found
     */
    fun findGameObjectByName(objectName: String, centerTile: Tile, range: Double): GameObject {
        return Objects.stream()
            .name(objectName)
            .within(centerTile, range)
            .nearest()
            .firstOrNull() ?: GameObject.Nil
    }

    /**
     * Interacts with a game object by name.
     * @param objectName The name of the object
     * @param action The action to perform
     * @return true if interaction was successful
     */
    fun interactWithObject(objectName: String, action: String): Boolean {
        val obj = Objects.stream().name(objectName).nearest().firstOrNull()
        if (obj != null && obj.valid()) {
            return obj.interact(action)
        }
        return false
    }

    fun isObjectNearby(objectName: String, radius: Int = 15): Boolean {
        return Objects.stream()
            .name(objectName)
            .within(Players.local(), radius)
            .isNotEmpty()
    }

    /**
     * Attempts to interact with a game object and waits for XP gain.
     *
     * @param obj The GameObject to interact with
     * @param action The action to perform
     * @param skill The skill to monitor for XP gain
     * @param xpTimeout Timeout for XP gain in milliseconds (default: 5000)
     * @param xpAttempts Max attempts for XP gain (default: 10)
     * @return True if the interaction was successful and XP was gained
     */
    fun interactAndWaitForXp(
        obj: GameObject,
        action: String,
        skill: org.powbot.api.rt4.walking.model.Skill,
        xpTimeout: Int = 5000,
        xpAttempts: Int = 10
    ): Boolean {
        if (!obj.valid()) {
            return false
        }

        val initialXp = Skills.experience(skill)

        return if (obj.interact(action)) {
            Condition.wait(
                { Skills.experience(skill) > initialXp },
                xpTimeout,
                xpAttempts
            )
        } else {
            false
        }
    }
}

