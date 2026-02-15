package org.powbot.community.squidharpooner

import org.powbot.api.Condition
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.Equipment
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
import org.powbot.community.api.ScriptLogging
import org.powbot.community.squidharpooner.tasks.FishTask
import org.powbot.community.squidharpooner.tasks.ProcessSquidTask

private const val FISHING_SPOT_NAME = "Fishing spot"
private const val HARPOON_ACTION = "Harpoon"
private const val KNIFE_NAME = "Knife"
private const val HARPOON_NAME = "Harpoon"
private const val RAW_SWORDTIP_SQUID = "Raw swordtip squid"
private const val RAW_JUMP_SQUID = "Raw jump squid"

@ScriptManifest(
    name = "0m6 Squid Harpooner",
    description = "Harpoons squid and cuts swordtip/jump squid with a knife when inventory is full",
    version = "1.0.0",
    author = "0m6",
    scriptId = "",
    category = ScriptCategory.Fishing
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            name = "Info",
            description = "Start at a Fishing spot with Knife and Harpoon (inventory or equipped)",
            optionType = OptionType.INFO
        )
    ]
)
class SquidHarpooner : AbstractScript() {

    var currentTask: String = "Initializing"
    var squidCaught: Int = 0
    private var startTime: Long = 0L
    private var lastRawSquidCount: Int = 0

    private val tasks: List<Task> by lazy {
        listOf(
            ProcessSquidTask(this, "Process Squid"),
            FishTask(this, "Harpoon Squid")
        )
    }

    override fun onStart() {
        ScriptLogging.action(logger, "Squid Harpooner starting - Version 1.0.0")
        startTime = System.currentTimeMillis()
        lastRawSquidCount = currentRawSquidCount()

        val paint = PaintBuilder.newBuilder()
            .x(40).y(80)
            .addString("Current Task") { currentTask }
            .addString("Squid Caught") { squidCaught.toString() }
            .addString("Squid/hr") { squidPerHour().toString() }
            .trackSkill(Skill.Fishing)
            .build()
        addPaint(paint)
    }

    override fun poll() {
        if (Inventory.stream().name(KNIFE_NAME).isEmpty()) {
            currentTask = "Stopping - Missing Knife"
            ScriptLogging.stopWithNotification(this, "Missing Knife - stopping script")
            Condition.sleep(400)
            return
        }

        if (Inventory.stream().name(HARPOON_NAME).isEmpty() && Equipment.stream().name(HARPOON_NAME).isEmpty()) {
            currentTask = "Stopping - Missing Harpoon"
            ScriptLogging.stopWithNotification(this, "Missing Harpoon - stopping script")
            Condition.sleep(400)
            return
        }

        updateSquidCaught()

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

        Condition.sleep(150)
    }

    fun shouldHarpoon(): Boolean {
        val player = Players.local()
        return !Inventory.isFull() && player.animation() == -1 && !player.inMotion()
    }

    fun rawSwordtipCount(): Int = Inventory.stream().name(RAW_SWORDTIP_SQUID).list().sumOf { it.stack }

    fun rawJumpCount(): Int = Inventory.stream().name(RAW_JUMP_SQUID).list().sumOf { it.stack }

    fun rawSquidCountByName(name: String): Int = Inventory.stream().name(name).list().sumOf { it.stack }

    fun findFishingSpot(): Npc? {
        return Npcs.stream()
            .name(FISHING_SPOT_NAME)
            .action(HARPOON_ACTION)
            .within(12)
            .nearest()
            .firstOrNull()
    }

    fun isFishingSpotWithinOneTile(): Boolean {
        val spot = findFishingSpot() ?: return false
        if (!spot.valid()) {
            return false
        }
        return Players.local().tile().distanceTo(spot.tile()) <= 1.0
    }

    fun harpoonNow(): Boolean {
        val spot = findFishingSpot()
        if (spot == null || !spot.valid()) {
            Condition.sleep(400)
            return false
        }

        if (!spot.inViewport()) {
            Camera.turnTo(spot)
            Condition.wait({ spot.inViewport() }, 250, 8)
            return false
        }

        if (!spot.interact(HARPOON_ACTION)) {
            Condition.sleep(150)
            return false
        }

        return Condition.wait({ Players.local().animation() != -1 }, 120, 20)
    }

    private fun currentRawSquidCount(): Int {
        return rawSwordtipCount() + rawJumpCount()
    }

    private fun updateSquidCaught() {
        val now = currentRawSquidCount()
        if (now > lastRawSquidCount) {
            squidCaught += (now - lastRawSquidCount)
        }
        lastRawSquidCount = now
    }

    private fun squidPerHour(): Int {
        val runtime = System.currentTimeMillis() - startTime
        if (runtime <= 0L) {
            return 0
        }
        return (squidCaught * 3_600_000L / runtime).toInt()
    }
}

fun main() {
    SquidHarpooner().startScript("127.0.0.1", "0m6", false)
}

