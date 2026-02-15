package org.powbot.community.herbrun.services

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.GameObject
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Players
import org.powbot.community.api.MovementUtils
import kotlin.random.Random

private const val BIG_COMPOST_BIN_NAME = "Big Compost Bin"
private const val VOLCANIC_ASH_NAME = "Volcanic ash"
private const val BUCKET_NAME = "Bucket"
private const val PINEAPPLE_NAME = "Pineapple"
private const val BIG_COMPOST_BUCKET_EMPTY_TIMEOUT_MS = 90_000L

class BigCompostBinService(
    private val bankTile: Tile,
    private val binTile: Tile,
    private val openBankWithRetries: (String) -> Boolean,
    private val depositInventoryWithRetries: () -> Boolean,
    private val withdrawByNameWithRetries: (String, Int) -> Boolean,
    private val isNearTile: (Tile) -> Boolean,
    private val isNearFarmingGuildBank: () -> Boolean,
    private val logInfo: (String) -> Unit,
    private val logWarn: (String) -> Unit,
    private val logError: (String) -> Unit,
    private val stopScript: (String) -> Unit,
    private val markComplete: () -> Unit
) {

    fun runPhase() {
        logInfo("Starting Big Compost Bin phase.")

        logInfo("Preparing initial Big Compost Bin supplies at Farming Guild bank.")
        if (!prepareBigCompostBanking(volcanicAsh = 50, buckets = 15, pineapples = 0)) return
        if (!walkToBigCompostBin()) {
            return
        }

        val bin = findBigCompostBin()
        if (bin == null || !bin.valid()) {
            logWarn("Big Compost Bin not found near target tile.")
            return
        }

        if (!openBigCompostBin()) return

        val ashApplied = useItemOnBigCompostBin(VOLCANIC_ASH_NAME)
        logInfo("Applied volcanic ash for Take: $ashApplied")

        val firstBucketBatch = useItemOnBigCompostBin(BUCKET_NAME)
        logInfo("Used first bucket batch on bin: $firstBucketBatch")
        if (!waitForInventoryItemZero(BUCKET_NAME, BIG_COMPOST_BUCKET_EMPTY_TIMEOUT_MS)) {
            logWarn("Timed out waiting for buckets to reach 0 after first take batch.")
            return
        }

        if (countInventoryItem(BUCKET_NAME) == 0) {
            if (!prepareBigCompostBanking(volcanicAsh = 0, buckets = 15, pineapples = 0)) return
            if (!walkToBigCompostBin()) return
            if (!openBigCompostBin()) return
            val secondBucketBatch = useItemOnBigCompostBin(BUCKET_NAME)
            logInfo("Used second bucket batch on bin: $secondBucketBatch")
            if (!waitForInventoryItemZero(BUCKET_NAME, BIG_COMPOST_BUCKET_EMPTY_TIMEOUT_MS)) {
                logWarn("Timed out waiting for buckets to reach 0 after second take batch.")
                return
            }
        }

        for (batch in 1..2) {
            if (!prepareBigCompostBanking(volcanicAsh = 0, buckets = 0, pineapples = 15)) return
            if (!walkToBigCompostBin()) return
            if (!openBigCompostBin()) return
            val filled = useItemOnBigCompostBin(PINEAPPLE_NAME)
            logInfo("Applied pineapple batch $batch/2: $filled")
            if (!waitForInventoryItemZero(PINEAPPLE_NAME, BIG_COMPOST_BUCKET_EMPTY_TIMEOUT_MS)) {
                logWarn("Timed out waiting for pineapples to reach 0 after batch $batch.")
                return
            }
        }

        val closed = closeBigCompostBinIfPossible()
        logInfo("Closed Big Compost Bin: $closed")
        markComplete()
        logInfo("Big Compost Bin phase complete.")
    }

    private fun walkToBigCompostBin(): Boolean {
        if (isNearTile(binTile)) {
            return true
        }
        MovementUtils.enableRunIfNeeded()
        logInfo("Walking to Big Compost Bin tile.")
        Movement.walkTo(binTile)
        val reached = Condition.wait({ isNearTile(binTile) }, 300, 25)
        if (!reached) {
            logWarn("Unable to reach Big Compost Bin tile.")
        }
        return reached
    }

    private fun findBigCompostBin(): GameObject? {
        return Objects.stream()
            .name(BIG_COMPOST_BIN_NAME)
            .within(binTile, 10.0)
            .nearest()
            .firstOrNull()
    }

    private fun hasObjectAction(obj: GameObject, action: String): Boolean {
        val actions = obj.actions()
        return actions.any { it.equals(action, ignoreCase = true) }
    }

    private fun openBigCompostBin(): Boolean {
        val bin = findBigCompostBin() ?: return false
        if (!hasObjectAction(bin, "Open")) {
            return true
        }
        val opened = bin.interact("Open")
        if (!opened) {
            logWarn("Failed to open Big Compost Bin.")
            return false
        }
        Condition.wait({ Players.local().animation() != -1 }, 120, 12)
        Condition.wait({ Players.local().animation() == -1 }, 120, 25)
        return true
    }

    private fun closeBigCompostBinIfPossible(): Boolean {
        val bin = findBigCompostBin() ?: return false
        if (!hasObjectAction(bin, "Close")) {
            return false
        }
        val closed = bin.interact("Close")
        if (closed) {
            Condition.wait({ Players.local().animation() != -1 }, 120, 12)
            Condition.wait({ Players.local().animation() == -1 }, 120, 25)
        }
        return closed
    }

    private fun prepareBigCompostBanking(volcanicAsh: Int, buckets: Int, pineapples: Int): Boolean {
        if (!isNearTile(bankTile)) {
            MovementUtils.enableRunIfNeeded()
            logInfo("Walking to Farming Guild bank for Big Compost Bin supplies.")
            Movement.walkTo(bankTile)
            if (!Condition.wait({ isNearTile(bankTile) }, 300, 25)) {
                logWarn("Failed to reach Farming Guild bank for Big Compost Bin supplies.")
                return false
            }
        }

        if (!Bank.opened() && !openBankWithRetries("big compost bin supplies")) {
            logWarn("Failed to open bank for Big Compost Bin supplies.")
            return false
        }
        if (!isNearFarmingGuildBank()) {
            logWarn("Opened bank is not at Farming Guild during Big Compost Bin phase.")
            return false
        }
        if (!depositInventoryWithRetries()) {
            logWarn("Failed to deposit inventory for Big Compost Bin supplies.")
            return false
        }
        if (volcanicAsh > 0 && !withdrawByNameWithRetries(VOLCANIC_ASH_NAME, volcanicAsh)) {
            val reason = "Missing required Big Compost Bin supply: $VOLCANIC_ASH_NAME x$volcanicAsh"
            logError(reason)
            stopScript(reason)
            return false
        }
        if (buckets > 0 && !withdrawByNameWithRetries(BUCKET_NAME, buckets)) {
            val reason = "Missing required Big Compost Bin supply: $BUCKET_NAME x$buckets"
            logError(reason)
            stopScript(reason)
            return false
        }
        if (pineapples > 0 && !withdrawByNameWithRetries(PINEAPPLE_NAME, pineapples)) {
            val reason = "Missing required Big Compost Bin supply: $PINEAPPLE_NAME x$pineapples"
            logError(reason)
            stopScript(reason)
            return false
        }
        Bank.close()
        Condition.wait({ !Bank.opened() }, 120, 12)
        return true
    }

    private fun useItemOnBigCompostBin(itemName: String): Boolean {
        val item = Inventory.stream().name(itemName).firstOrNull() ?: return false
        val beforeCount = countInventoryItem(itemName)
        if (!item.interact("Use")) {
            return false
        }
        Condition.sleep(Random.nextInt(120, 220))
        val bin = findBigCompostBin() ?: return false
        val interacted = bin.interact("Use") || bin.click()
        if (!interacted) {
            return false
        }
        Condition.wait({ countInventoryItem(itemName) < beforeCount }, 200, 20)
        return true
    }

    private fun countInventoryItem(name: String): Int {
        return Inventory.stream().name(name).count(true).toInt()
    }

    private fun waitForInventoryItemZero(name: String, timeoutMs: Long): Boolean {
        val polls = (timeoutMs / 300L).coerceAtLeast(1L).toInt()
        return Condition.wait({ countInventoryItem(name) == 0 }, 300, polls)
    }
}

