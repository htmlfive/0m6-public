package org.powbot.community.lumbyfires

import org.powbot.api.Condition
import org.powbot.api.Notifications
import org.powbot.api.Tile
import org.powbot.api.rt4.GroundItems
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Skills
import org.powbot.api.rt4.World
import org.powbot.api.rt4.Worlds
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.AbstractScript
import org.powbot.api.script.ScriptConfiguration
import org.powbot.api.script.ScriptManifest
import org.powbot.api.script.ScriptCategory
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.mobile.script.ScriptManager
import kotlin.random.Random

@ScriptManifest(
    name = "LumbyFires",
    description = "Lights all available logs in Lumbridge, then hops to a non-specialty world when no logs remain.",
    version = "1.0.0",
    author = "0m6",
    scriptId = "",
    category = ScriptCategory.Firemaking
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            name = "Info",
            description = "Start near the Lumbridge logs with a tinderbox in your inventory.",
            optionType = org.powbot.api.script.OptionType.INFO
        )
    ]
)
class LumbyFires : AbstractScript() {

    companion object {
        private val LOG_NAMES = arrayOf("Logs")
        private const val TINDERBOX_NAME = "Tinderbox"
        private const val LOG_SEARCH_RADIUS = 10
        private const val FIRE_OBJECT_ID = 26185
        private const val FIRE_FREE_TILE_RADIUS = 3
    }

    private var totalFiresLit = 0
    private var worldsHopped = 0
    private var currentAction = "Initializing"

    override fun onStart() {
        currentAction = "Preparing"
        val paint = PaintBuilder.newBuilder()
            .x(30)
            .y(60)
            .trackSkill(Skill.Firemaking)
            .addString("Action") { currentAction }
            .addString("Fires total") { totalFiresLit.toString() }
            .addString("Worlds hopped") { worldsHopped.toString() }
            .addString("Current world") { Worlds.current().number.toString() }
            .build()
        addPaint(paint)
        logger.info("LumbyFires initialized.")
    }

    override fun poll() {
        if (Skills.realLevel(Skill.Firemaking) == 15) {
            Notifications.showNotification("LumbyFires: Reached level 15. Stopping")
            logger.info("LumbyFires: Reached level 15. Stopping")
            ScriptManager.stop()
        }
        if (!hasTinderbox()) {
            currentAction = "Need tinderbox - stopping"
            Notifications.showNotification("LumbyFires: Bring a tinderbox to keep training.")
            logger.error("Missing tinderbox, stopping script.")
            ScriptManager.stop()
            Condition.sleep(1000)
            return
        }

        if (!hasLogInInventory()) {
            currentAction = "Picking up log"
            if (!pickupLogFromGround()) {
                // No logs on ground and none in inventory - time to hop
                if (!hasLogsAvailableOnGround()) {
                    if (!hopToNextWorld()) {
                        currentAction = "Waiting to hop"
                        Condition.sleep(600)
                    }
                } else {
                    Condition.sleep(400)
                }
            }
            return
        }

        if (isStandingOnFire()) {
            currentAction = "Repositioning off fire"
            if (!moveToNearestFireFreeTile()) {
                logger.warn("Failed to move off fire tile ${Players.local().tile()}")
                Condition.sleep(400)
            }
            return
        }

        currentAction = "Lighting fire"
        if (!lightCurrentLog()) {
            Condition.sleep(400)
        }
    }

    private fun hasTinderbox(): Boolean {
        return Inventory.stream().name(TINDERBOX_NAME).count() > 0
    }

    private fun hasLogInInventory(): Boolean {
        return Inventory.stream().name(*LOG_NAMES).count() > 0
    }

    private fun hasLogsAvailableOnGround(): Boolean {
        return GroundItems.stream()
            .name(*LOG_NAMES)
            .within(Players.local(), LOG_SEARCH_RADIUS)
            .isNotEmpty()
    }

    private fun pickupLogFromGround(): Boolean {
        val log = GroundItems.stream()
            .name(*LOG_NAMES)
            .within(Players.local(), LOG_SEARCH_RADIUS)
            .toList()
            .minByOrNull { it.distance() } ?: return false

        val beforeLogCount = Inventory.stream().name(*LOG_NAMES).count()
        if (!log.interact("Take")) {
            return false
        }

        val pickedUp = Condition.wait(
            { Inventory.stream().name(*LOG_NAMES).count() > beforeLogCount },
            200,
            15
        )

        if (!pickedUp) {
            logger.warn("Failed to pick up log ${log.name()} at ${log.tile()}")
        }

        return pickedUp
    }

    private fun lightCurrentLog(): Boolean {
        val tinderbox = Inventory.stream().name(TINDERBOX_NAME).firstOrNull() ?: return false
        val log = Inventory.stream().name(*LOG_NAMES).firstOrNull() ?: return false

        if (isStandingOnFire()) {
            currentAction = "Repositioning off fire"
            if (!moveToNearestFireFreeTile()) {
                logger.warn("Failed to move off fire tile ${Players.local().tile()}")
                return false
            }
        }

        val startingXp = Skills.experience(Skill.Firemaking)
        if (!tinderbox.interact("Use")) {
            return false
        }

        Condition.sleep(Random.nextInt(80, 120))

        if (!log.interact("Use")) {
            return false
        }

        val gainedXp = Condition.wait(
            { Skills.experience(Skill.Firemaking) > startingXp },
            300,
            25
        )

        if (gainedXp) {
            totalFiresLit++
            Condition.sleep(Random.nextInt(400, 650))
        } else {
            logger.warn("Did not detect firemaking XP after using log.")
        }

        return gainedXp
    }

    private fun hopToNextWorld(): Boolean {
        val nextWorld = findNextWorld() ?: return false
        val targetWorldNumber = nextWorld.number

        currentAction = "Hopping -> $targetWorldNumber"
        logger.info("Hopping from world ${Worlds.current().number} to $targetWorldNumber.")

        val hopped = if (nextWorld.hop()) {
            Condition.wait(
                { Worlds.current().number == targetWorldNumber },
                300,
                40
            )
        } else {
            false
        }

        if (hopped) {
            worldsHopped++
            Condition.sleep(Random.nextInt(900, 1400))
        } else {
            logger.warn("World hop to $targetWorldNumber failed.")
        }

        return hopped
    }

    private fun findNextWorld(): World? {
        val currentWorld = Worlds.current()
        val availableWorlds = Worlds.stream()
            .filtered { it.type == currentWorld.type && it.specialty == World.Specialty.NONE }
            .toList()
            .sortedBy { it.number }

        if (availableWorlds.isEmpty()) {
            logger.warn("No non-specialty worlds available.")
            return null
        }

        val currentIndex = availableWorlds.indexOfFirst { it.number == currentWorld.number }
        if (currentIndex == -1) {
            return availableWorlds.first()
        }

        val nextIndex = (currentIndex + 1) % availableWorlds.size
        return availableWorlds[nextIndex]
    }

    private fun isStandingOnFire(): Boolean {
        return isTileOnFire(Players.local().tile())
    }

    private fun isTileOnFire(tile: Tile): Boolean {
        return Objects.stream()
            .id(FIRE_OBJECT_ID)
            .filtered { it.tile() == tile }
            .isNotEmpty()
    }

    private fun moveToNearestFireFreeTile(): Boolean {
        val destination = findNearestFireFreeTile(Players.local().tile()) ?: return false
        if (!Movement.step(destination)) {
            return false
        }

        return Condition.wait(
            {
                Players.local().tile().distanceTo(destination) <= 0.0 &&
                    !isTileOnFire(destination)
            },
            200,
            20
        )
    }

    private fun findNearestFireFreeTile(origin: Tile): Tile? {
        val candidates = mutableListOf<Tile>()

        for (dx in -FIRE_FREE_TILE_RADIUS..FIRE_FREE_TILE_RADIUS) {
            for (dy in -FIRE_FREE_TILE_RADIUS..FIRE_FREE_TILE_RADIUS) {
                if (dx == 0 && dy == 0) {
                    continue
                }
                val candidate = origin.derive(dx, dy)
                if (isTileOnFire(candidate)) {
                    continue
                }
                if (!Movement.reachable(Players.local(), candidate)) {
                    continue
                }
                candidates.add(candidate)
            }
        }

        return candidates.minByOrNull { origin.distanceTo(it) }
    }
}


fun main() {
    val script = LumbyFires()
    script.startScript("localhost", "0m6", false)
}

