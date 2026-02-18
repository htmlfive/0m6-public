package org.powbot.community.wcplanker

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.script.AbstractScript
import org.powbot.api.script.OptionType
import org.powbot.api.script.ScriptCategory
import org.powbot.api.script.ScriptConfiguration
import org.powbot.api.script.ScriptManifest
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Components
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Npcs
import org.powbot.api.rt4.Players
import org.powbot.community.api.ScriptLogging

@ScriptManifest(
    name = "0m6 WC Guild Planker",
    description = "Withdraw logs, run to sawmill, buy planks, return to bank, and deposit planks.",
    version = "1.0.0",
    author = "0m6",
    scriptId = "b2cca274-dec6-47e5-8d5a-79687ae6174a",
    category = ScriptCategory.Construction
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            name = "Info",
            description = "Start anywhere with coins in bank/inventory. Script banks at 1592,3476,0 and sawmills at 1624,3500,0.",
            optionType = OptionType.INFO
        ),
        ScriptConfiguration(
            name = "Log Name",
            description = "Log type to withdraw and convert to planks.",
            optionType = OptionType.STRING,
            defaultValue = "Teak logs",
            allowedValues = [
                "Logs",
                "Oak logs",
                "Teak logs",
                "Mahogany logs",
                "Camphor logs",
                "Ironwood logs",
                "Rosewood logs"
            ]
        ),
        ScriptConfiguration(
            name = "Stop At Planks",
            description = "Stop after making this many planks (0 = unlimited).",
            optionType = OptionType.INTEGER,
            defaultValue = "0"
        )
    ]
)
class WcPlanker : AbstractScript() {

    private companion object {
        val BANK_TILE = Tile(1592, 3476, 0)
        val SAWMILL_TILE = Tile(1624, 3500, 0)
        const val SAWMILL_NPC = "Sawmill operator"
        const val SAWMILL_WIDGET_ID = 270
        const val BANK_DISTANCE = 6.0
        const val SAWMILL_DISTANCE = 7.0
    }

    private var configuredLogName: String = "Teak logs"
    private var selectedLogKeyword: String = "Teak"
    private var currentTask: String = "Initializing"
    private var stopAtPlanks: Int = 0
    private var planksMade: Int = 0
    private var startedAtMs: Long = 0L
    private var costPerPlank: Int = 500

    override fun onStart() {
        startedAtMs = System.currentTimeMillis()
        configuredLogName = getOption<String>("Log Name").trim().ifBlank { "Teak logs" }
        stopAtPlanks = getOption<Int>("Stop At Planks").coerceAtLeast(0)
        selectedLogKeyword = toLogKeyword(configuredLogName)
        costPerPlank = costForLog(configuredLogName)

        addPaint(
            PaintBuilder.newBuilder()
                .x(30)
                .y(80)
                .addString("Task") { currentTask }
                .addString("Log") { configuredLogName }
                .addString("Match") { selectedLogKeyword }
                .addString("Cost/Plank") { costPerPlank.toString() }
                .addString("Trip Cost") { (logCount() * costPerPlank).toString() }
                .addString("Planks Made") { planksMade.toString() }
                .addString("Planks/hr") { planksPerHour().toString() }
                .addString("Stop At") { if (stopAtPlanks == 0) "Unlimited" else stopAtPlanks.toString() }
                .build()
        )

        ScriptLogging.info(
            logger,
            "Starting WC Planker with log='$configuredLogName' keyword='$selectedLogKeyword' cost=$costPerPlank stopAt=$stopAtPlanks"
        )
    }

    override fun poll() {
        if (stopAtPlanks > 0 && planksMade >= stopAtPlanks) {
            ScriptLogging.stopWithNotification(this, "Reached plank goal: $planksMade/$stopAtPlanks")
            return
        }

        refreshLogKeywordFromInventory()

        if (shouldGoBanking()) {
            handleBanking()
            return
        }

        if (!isNearSawmill()) {
            currentTask = "Webwalking to sawmill"
            Movement.walkTo(SAWMILL_TILE)
            Condition.sleep(200)
            return
        }

        processSawmill()
    }

    private fun shouldGoBanking(): Boolean {
        val hasLogs = inventoryHasLogs()
        val hasPlanks = inventoryHasPlanks()
        return !hasLogs || hasPlanks
    }

    private fun handleBanking() {
        if (!isNearBank()) {
            currentTask = "Webwalking to bank"
            Movement.walkTo(BANK_TILE)
            Condition.sleep(200)
            return
        }

        if (!Bank.opened()) {
            currentTask = "Opening bank"
            if (Bank.open()) {
                Condition.wait({ Bank.opened() }, 120, 20)
            }
            return
        }

        currentTask = "Depositing and withdrawing logs"
        if (Inventory.stream().nameContains("plank").isNotEmpty()) {
            Bank.depositAllExcept("Coins")
            Condition.wait({ Inventory.stream().nameContains("plank").isEmpty() }, 120, 20)
        }
        if (!hasSelectedLogsInBank()) {
            ScriptLogging.stopWithNotification(this, "Out of logs: $configuredLogName")
            return
        }

        val freeSlots = Inventory.emptySlotCount()
        if (freeSlots <= 0) {
            Bank.close()
            Condition.wait({ !Bank.opened() }, 80, 12)
            return
        }

        val withdrew = Bank.withdraw(configuredLogName, freeSlots)
        if (!withdrew) {
            if (Inventory.stream().name(configuredLogName).isEmpty()) {
                ScriptLogging.stopWithNotification(this, "No '$configuredLogName' found in bank. Stopping.")
            }
            return
        }

        val received = Condition.wait(
            { Inventory.stream().name(configuredLogName).isNotEmpty() },
            120,
            20
        )
        if (!received) {
            ScriptLogging.warn(logger, "Withdraw call succeeded but inventory did not update for $configuredLogName")
            return
        }

        refreshLogKeywordFromInventory()
        Bank.close()
        Condition.wait({ !Bank.opened() }, 80, 12)
    }

    private fun processSawmill() {
        val logsBefore = logCount()
        if (logsBefore <= 0) {
            return
        }
        val requiredGp = logsBefore.toLong() * costPerPlank.toLong()
        val coins = coinsInInventory()
        if (requiredGp > coins) {
            ScriptLogging.stopWithNotification(
                this,
                "Not enough gp: needs $requiredGp, have $coins"
            )
            return
        }

        currentTask = "Using Buy-plank"
        val sawmillOperator = Npcs.stream()
            .name(SAWMILL_NPC)
            .nearest()
            .firstOrNull()

        if (sawmillOperator == null || !sawmillOperator.valid()) {
            Condition.sleep(250)
            return
        }

        if (!sawmillOperator.interact("Buy-plank")) {
            Condition.sleep(200)
            return
        }

        val componentClicked = Condition.wait({
            clickPlankOptionForKeyword(selectedLogKeyword)
        }, 120, 10)

        if (!componentClicked) {
            ScriptLogging.warn(logger, "Could not find sawmill component containing '$selectedLogKeyword'")
            return
        }

        Condition.wait({ logCount() < logsBefore }, 120, 30)
        val logsAfter = logCount()
        if (logsAfter < logsBefore) {
            planksMade += (logsBefore - logsAfter)
        }
    }

    private fun clickPlankOptionForKeyword(keyword: String): Boolean {
        if (keyword.isBlank()) return false
        val normalizedKeyword = normalizeForMatch(keyword)
        val shortKeyword = normalizedKeyword.take(5)

        val option = Components.stream(SAWMILL_WIDGET_ID)
            .filtered {
                if (!it.visible()) return@filtered false
                val normalizedText = normalizeForMatch(it.text())
                val normalizedName = normalizeForMatch(it.name())
                normalizedText.contains(normalizedKeyword) ||
                    normalizedName.contains(normalizedKeyword) ||
                    (shortKeyword.length >= 3 && (
                        normalizedText.contains(shortKeyword) ||
                            normalizedName.contains(shortKeyword)
                        ))
            }
            .firstOrNull()
            ?: return false

        val preferredAction = option.actions().firstOrNull { action ->
            action.contains("make", ignoreCase = true) ||
            action.contains("all", ignoreCase = true) ||
                action.contains("buy", ignoreCase = true)
        }

        return if (preferredAction != null) {
            option.interact(preferredAction)
        } else {
            option.click()
        }
    }

    private fun refreshLogKeywordFromInventory() {
        val heldLogs = Inventory.stream()
            .nameContains("logs")
            .firstOrNull()
            ?: return

        val heldName = heldLogs.name().trim()
        if (heldName.isBlank()) return

        selectedLogKeyword = toLogKeyword(heldName)
    }

    private fun inventoryHasLogs(): Boolean {
        return Inventory.stream().nameContains("logs").isNotEmpty()
    }

    private fun inventoryHasPlanks(): Boolean {
        return Inventory.stream().nameContains("plank").isNotEmpty()
    }

    private fun logCount(): Int {
        return Inventory.stream().nameContains("logs").count(true).toInt()
    }

    private fun isNearBank(): Boolean {
        return Players.local().tile().distanceTo(BANK_TILE) <= BANK_DISTANCE
    }

    private fun isNearSawmill(): Boolean {
        return Players.local().tile().distanceTo(SAWMILL_TILE) <= SAWMILL_DISTANCE
    }

    private fun toLogKeyword(logName: String): String {
        val trimmed = logName.trim()
        val suffix = " logs"
        return if (trimmed.endsWith(suffix, ignoreCase = true)) {
            trimmed.substring(0, trimmed.length - suffix.length).trim()
        } else {
            trimmed
        }
    }

    private fun normalizeForMatch(value: String): String {
        return value.lowercase().replace(Regex("[^a-z0-9]"), "")
    }

    private fun planksPerHour(): Int {
        val elapsedMs = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(1L)
        return ((planksMade.toDouble() * 3_600_000.0) / elapsedMs).toInt()
    }

    private fun costForLog(logName: String): Int {
        return when (logName.trim().lowercase()) {
            "logs" -> 100
            "oak logs" -> 250
            "teak logs" -> 500
            "mahogany logs" -> 1_500
            "camphor logs" -> 2_500
            "ironwood logs" -> 5_000
            "rosewood logs" -> 7_500
            else -> 500
        }
    }

    private fun coinsInInventory(): Long {
        return Inventory.stream()
            .name("Coins")
            .firstOrNull()
            ?.stackSize()
            ?.toLong()
            ?: 0L
    }

    private fun hasSelectedLogsInBank(): Boolean {
        return Bank.stream().name(configuredLogName).isNotEmpty()
    }
}

fun main() {
    val script = WcPlanker()
    script.startScript("localhost", "0m6", false)
}
