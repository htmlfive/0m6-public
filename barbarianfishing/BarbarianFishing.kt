package org.powbot.om6.barbarianfishing

import com.google.common.eventbus.Subscribe
import org.powbot.api.Condition
import org.powbot.api.event.PaintCheckboxChangedEvent
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Npc
import org.powbot.api.rt4.Npcs
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.AbstractScript
import org.powbot.api.script.OptionType
import org.powbot.api.script.ScriptCategory
import org.powbot.api.script.ScriptConfiguration
import org.powbot.api.script.ScriptManifest
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.om6.api.BlockingWorldHopService
import org.powbot.om6.api.ScriptLogging
import org.powbot.om6.api.WorldHopIntervalScheduler
import org.powbot.om6.barbarianfishing.tasks.CutLeapingTask
import org.powbot.om6.barbarianfishing.tasks.DropRoeTask
import org.powbot.om6.barbarianfishing.tasks.EatRoeTask
import org.powbot.om6.barbarianfishing.tasks.FishTask
import kotlin.random.Random

private const val OPTION_WORLD_HOP_MIN_MINUTES = "World Hop Min Minutes"
private const val OPTION_WORLD_HOP_MAX_MINUTES = "World Hop Max Minutes"
private const val HOP_NOW_CHECKBOX_ID = "barbarian_fishing_hop_now_checkbox"

@ScriptManifest(
    name = "0m6 Barbarian Fishing",
    description = "Uses rod on Fishing spot and cuts all Leaping fish with a knife",
    version = "1.0.0",
    author = "0m6",
    scriptId = "",
    category = ScriptCategory.Fishing
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            name = "Info",
            description = "Start near barbarian Fishing spot with Barbarian rod, feathers, and Knife",
            optionType = OptionType.INFO
        ),
        ScriptConfiguration(
            name = OPTION_WORLD_HOP_MIN_MINUTES,
            description = "Random timer lower bound for world hopping in minutes.",
            optionType = OptionType.STRING,
            defaultValue = "30"
        ),
        ScriptConfiguration(
            name = OPTION_WORLD_HOP_MAX_MINUTES,
            description = "Random timer upper bound for world hopping in minutes.",
            optionType = OptionType.STRING,
            defaultValue = "60"
        )
    ]
)
class BarbarianFishing : AbstractScript() {

    var currentTask: String = "Initializing"
    var nextRoeEatAt: Long = 0L
    var nextEatAt: Long = 0L
    var nextCutAt: Long = 0L
    var worldHopInProgress: Boolean = false
    private var worldHopMinMinutes: Int = 30
    private var worldHopMaxMinutes: Int = 60
    private val worldHopScheduler = WorldHopIntervalScheduler(30, 60)
    private var hopNowRequested: Boolean = false
    // Reusable API service: inject logger lambdas and call hopToPreferredWorldWithRetries when needed.
    private val worldHopService = BlockingWorldHopService(
        onInfo = { ScriptLogging.info(logger, it) },
        onWarn = { ScriptLogging.warn(logger, it) }
    )

    private val tasks: List<Task> by lazy {
        listOf(
            DropRoeTask(this, "Drop Roe"),
            CutLeapingTask(this, "Cut Leaping Fish"),
            EatRoeTask(this, "Eat Roe"),
            FishTask(this, "Fish")
        )
    }

    override fun onStart() {
        ScriptLogging.action(logger, "Barbarian Fishing starting - Version 1.0.0")
        worldHopMinMinutes = parseWorldHopMinutes(getOption<String>(OPTION_WORLD_HOP_MIN_MINUTES), 30)
        worldHopMaxMinutes = parseWorldHopMinutes(getOption<String>(OPTION_WORLD_HOP_MAX_MINUTES), 60)
        worldHopScheduler.configure(worldHopMinMinutes, worldHopMaxMinutes)
        scheduleNextWorldHop()

        val paint = PaintBuilder.newBuilder()
            .x(40).y(80)
            .addString("Current Task") { currentTask }
            .addString("Next Hop In") { worldHopScheduler.countdownText() }
            .addCheckbox("Hop now", HOP_NOW_CHECKBOX_ID, false)
            .trackSkill(Skill.Fishing)
            .trackSkill(Skill.Agility)
            .trackSkill(Skill.Strength)
            .trackSkill(Skill.Cooking)
            .build()
        addPaint(paint)
    }

    override fun poll() {
        if (shouldHopWorldsNow()) {
            hopNowRequested = false
            worldHopInProgress = true
            currentTask = "World Hop"
            val hopped = worldHopService.hopToPreferredWorldWithRetries("Barbarian Fishing timer hop")
            worldHopInProgress = false
            if (hopped) {
                onWorldHopCompleted()
            }
            return
        }

        if (Inventory.stream().name("Feather").isEmpty()) {
            currentTask = "Stopping - Missing Feather"
            ScriptLogging.stopWithNotification(this, "Missing Feather - stopping script")
            Condition.sleep(400)
            return
        }

        if (Inventory.stream().name("Fly fishing rod").isEmpty()) {
            currentTask = "Stopping - Missing Fly fishing rod"
            ScriptLogging.stopWithNotification(this, "Missing Fly fishing rod - stopping script")
            Condition.sleep(400)
            return
        }

        if (Inventory.stream().name("Knife").isEmpty()) {
            currentTask = "Stopping - Missing Knife"
            ScriptLogging.stopWithNotification(this, "Missing Knife - stopping script")
            Condition.sleep(400)
            return
        }

        for (task in tasks) {
            if (!task.activate()) {
                continue
            }

            if (currentTask != task.name) {
                currentTask = task.name
            }

            task.execute()
            return
        }

        Condition.sleep(200)
    }

    fun findFishingSpot(): Npc? {
        return Npcs.stream()
            .name("Fishing spot")
            .action("Use-rod")
            .within(12)
            .nearest()
            .firstOrNull()
    }

    fun fishNow(waitForAnimation: Boolean = true): Boolean {
        val spot = findFishingSpot()
        if (spot == null || !spot.valid()) {
            ScriptLogging.warn(logger, "No Fishing spot with Use-rod nearby")
            Condition.sleep(600)
            return false
        }

        val distanceToSpot = Players.local().tile().distanceTo(spot.tile())
        val shouldRefish = distanceToSpot != 1.0

        if (waitForAnimation && Players.local().animation() != -1 && !shouldRefish) {
            return true
        }

        if (!spot.inViewport()) {
            Camera.turnTo(spot)
            Condition.wait({ spot.inViewport() }, 300, 8)
            return false
        }

        if (!spot.click()) {
            ScriptLogging.warn(logger, "Failed to click Fishing spot")
            Condition.sleep(250)
            return false
        }
        nextRoeEatAt = System.currentTimeMillis() + Random.nextLong(1200L, 1801L)

        if (!waitForAnimation) {
            return true
        }

        return Condition.wait({ Players.local().animation() != -1 }, 200, 30)
    }

    @Subscribe
    @Suppress("unused")
    fun onPaintCheckboxChanged(evt: PaintCheckboxChangedEvent) {
        if (evt.checkboxId == HOP_NOW_CHECKBOX_ID && evt.checked) {
            hopNowRequested = true
            worldHopScheduler.forceNow()
            ScriptLogging.info(logger, "PAINT: Hop now requested")
        }
    }

    private fun shouldHopWorldsNow(): Boolean {
        if (worldHopInProgress) {
            return false
        }
        return hopNowRequested || worldHopScheduler.shouldHopNow()
    }

    private fun onWorldHopCompleted() {
        scheduleNextWorldHop()
        nextRoeEatAt = System.currentTimeMillis() + Random.nextLong(1200L, 1801L)
    }

    private fun scheduleNextWorldHop() {
        worldHopScheduler.scheduleNext()
    }

    private fun parseWorldHopMinutes(value: String, fallback: Int): Int {
        val parsed = value.trim().toIntOrNull() ?: return fallback
        return parsed.coerceAtLeast(1)
    }
}

fun main() {
    BarbarianFishing().startScript("127.0.0.1", "0m6", false)
}
