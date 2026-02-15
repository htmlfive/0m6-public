package org.powbot.om6.mortmyre

import org.powbot.api.Condition
import org.powbot.api.rt4.Equipment
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Prayer
import org.powbot.api.script.AbstractScript
import org.powbot.api.script.OptionType
import org.powbot.api.script.ScriptCategory
import org.powbot.api.script.ScriptConfiguration
import org.powbot.api.script.ScriptManifest
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.om6.api.ScriptLogging
import org.powbot.om6.api.feroxpool.FeroxPoolConstants
import org.powbot.om6.mortmyre.config.Constants
import org.powbot.om6.mortmyre.tasks.BankAndGearTask
import org.powbot.om6.mortmyre.tasks.DrinkFromPoolTask
import org.powbot.om6.mortmyre.tasks.HarvestFungusTask
import org.powbot.om6.mortmyre.tasks.HopIfOccupiedTask
import org.powbot.om6.mortmyre.tasks.WalkToBloomTileTask
import org.powbot.om6.mortmyre.tasks.WalkToRefreshPoolTask

@ScriptManifest(
    name = "0m6 Mort Myre Fungus Harvester",
    description = "Harvests Mort myre fungus via Bloom, restores prayer at pool, and banks.",
    version = "1.0.1",
    author = "0m6",
    category = ScriptCategory.Other
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            name = "Info",
            description = "Start with Silver sickle (b) and path access to bloom tile/pool.",
            optionType = OptionType.INFO
        ),
        ScriptConfiguration(
            name = Constants.STOP_AT_OPTION,
            description = "Stop after collecting X Mort myre fungus (0 = unlimited).",
            defaultValue = "0",
            optionType = OptionType.INTEGER
        )
    ]
)
class MortMyreFungusHarvester : AbstractScript() {

    val stopAtFungus: Int
        get() = getOption<Int>(Constants.STOP_AT_OPTION)

    var currentTask: String = "Initializing"
    var fungusCollected: Int = 0

    private val tasks: List<Task> by lazy {
        listOf(
            WalkToRefreshPoolTask(this, "Walking to Pool"),
            DrinkFromPoolTask(this, "Drinking Pool"),
            BankAndGearTask(this, "Banking and Ring Setup"),
            HopIfOccupiedTask(this, "World Hopping - Bloom Tile Occupied"),
            WalkToBloomTileTask(this, "Walking to Bloom Tile"),
            HarvestFungusTask(this, "Casting Bloom and Picking Fungus")
        )
    }

    override fun onStart() {
        logger.info("Mort Myre Fungus Harvester starting.")
        logger.info("Stop At Fungus: $stopAtFungus")

        addPaint(
            PaintBuilder.newBuilder()
                .x(40)
                .y(80)
                .addString("Task") { currentTask }
                .addString("Fungus Collected") { fungusCollected.toString() }
                .trackInventoryItems(Constants.MORT_MYRE_FUNGUS_ID)
                .build()
        )
    }

    override fun poll() {
        if (shouldStopAtGoal()) {
            currentTask = "Stopping - Goal Reached"
            ScriptLogging.stopWithNotification(this, "Goal reached: $fungusCollected fungus")
            Condition.sleep(1000)
            return
        }

        val task = tasks.firstOrNull { it.activate() }
        if (task != null) {
            if (currentTask != task.name) {
                currentTask = task.name
            }
            task.execute()
            return
        }

        Condition.sleep(120)
    }

    override fun canBreak(): Boolean {
        return Players.local().tile().distanceTo(FeroxPoolConstants.BANK_TILE) <= 5
    }

    fun shouldReturnToPool(): Boolean {
        return Inventory.isFull() || isPrayerDepleted()
    }

    fun isPrayerDepleted(): Boolean = Prayer.prayerPoints() <= 0

    fun isAtBloomTile(): Boolean = Players.local().tile() == Constants.BLOOM_TILE

    fun isNearPoolTile(distance: Int = 6): Boolean {
        return Players.local().tile().distanceTo(Constants.POOL_TILE) <= distance
    }

    fun hasFungusInInventory(): Boolean {
        return Inventory.stream().name(Constants.FUNGUS_NAME).isNotEmpty()
    }

    fun isRingEquipped(): Boolean {
        val ring = Equipment.itemAt(Equipment.Slot.RING)
        return ring.valid() && ring.name().contains(Constants.RING_NAME_CONTAINS, ignoreCase = true)
    }

    fun needsRingAction(): Boolean = !isRingEquipped()

    fun hasNearbyPickableFungus(): Boolean {
        return Objects.stream()
            .name(Constants.BLOOM_OBJECT_NAME)
            .action(Constants.PICK_ACTION)
            .within(Players.local().tile(), 5.0)
            .nearest()
            .first()
            .valid()
    }

    fun hasPlayersNearBloomTile(): Boolean {
        val localPlayer = Players.local()
        return Players.stream()
            .within(Constants.BLOOM_TILE, Constants.BLOOM_HOP_RADIUS.toDouble())
            .filtered { it != localPlayer }
            .isNotEmpty()
    }

    private fun shouldStopAtGoal(): Boolean {
        return stopAtFungus > 0 && fungusCollected >= stopAtFungus
    }
}


fun main() {
    val script = MortMyreFungusHarvester()
    script.startScript("localhost", "0m6", false)
}
