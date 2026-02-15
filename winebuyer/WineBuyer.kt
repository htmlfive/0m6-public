package org.powbot.om6.winebuyer

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.script.AbstractScript
import org.powbot.api.script.ScriptCategory
import org.powbot.api.script.ScriptConfiguration
import org.powbot.api.script.ScriptManifest
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Npcs
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Store
import org.powbot.api.rt4.bank.Quantity
import kotlin.math.max
import kotlin.random.Random

@ScriptManifest(
    name = "0m6 Wine Buyer",
    description = "Trades Brikka, buys jugs of wine, banks, and repeats.",
    version = "1.0.0",
    author = "0m6",
    category = ScriptCategory.Other
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            "Info",
            "Start near Brikka with coins. Script buys wine until full inventory, then banks.",
            optionType = org.powbot.api.script.OptionType.INFO
        )
    ]
)
class WineBuyer : AbstractScript() {

    private companion object {
        const val BRIKKA = "Brikka"
        const val JUG_OF_WINE = "Jug of wine"
        const val JUG_OF_WINE_ID = 1993
        const val BUY_TEN = 10
        val FIXED_BANK_TILE = Tile(1936, 2755, 0)
        const val FIXED_BANK_RADIUS = 10.0
    }

    private var currentTask = "Initializing"
    private var inventoryTrips = 0
    private var totalBought = 0

    override fun onStart() {
        addPaint(
            PaintBuilder.newBuilder()
                .x(40).y(80)
                .addString("Current Task") { currentTask }
                .addString("Inventory Trips") { inventoryTrips.toString() }
                .addString("Wine Bought") { totalBought.toString() }
                .trackInventoryItems(JUG_OF_WINE_ID)
                .build()
        )
    }

    override fun poll() {
        if (Bank.opened()) {
            handleBanking()
            return
        }

        if (Inventory.isFull()) {
            currentTask = "Opening fixed bank"
            if (Store.opened()) {
                Store.close()
                Condition.wait({ !Store.opened() }, 50, 30)
            }

            if (!isAtFixedBank()) {
                currentTask = "Waiting at fixed bank"
                Condition.sleep(Random.nextInt(120, 220))
                return
            }

            if (openFixedBank()) {
                Condition.wait({ Bank.opened() }, 50, 200)
            }
            Condition.sleep(Random.nextInt(120, 220))
            return
        }

        currentTask = "Buying wine"
        buyWineBatch()
        Condition.sleep(Random.nextInt(100, 200))
    }

    private fun buyWineBatch() {
        if (!Store.opened()) {
            currentTask = "Trading Brikka"
            if (Store.open(BRIKKA)) {
                Condition.wait({ Store.opened() }, 50, 40)
            }
            return
        }

        if (Store.buyQuantity != BUY_TEN) {
            Store.setBuyQuantity(BUY_TEN)
            Condition.wait({ Store.buyQuantity == BUY_TEN }, 50, 20)
        }

        var unchangedAttempts = 0
        while (Store.opened() && !Inventory.isFull()) {
            val wineItem = Store.getItem(JUG_OF_WINE_ID)
            if (!wineItem.valid()) {
                logger.warn("Jug of wine is not visible in the store.")
                return
            }

            val before = wineCount()
            val clicked = wineItem.click()
            if (!clicked) {
                unchangedAttempts++
                if (unchangedAttempts >= 8) {
                    logger.warn("Could not click Jug of wine in store.")
                    return
                }
                Condition.sleep(Random.nextInt(80, 151))
                continue
            }

            val changed = Condition.wait({ wineCount() > before || Inventory.isFull() }, 50, 10)
            if (changed) {
                totalBought += max(0, wineCount() - before)
                unchangedAttempts = 0
            } else {
                unchangedAttempts++
            }

            if (unchangedAttempts >= 8) {
                logger.warn("No wine purchase change detected. Check stock/coins.")
                return
            }

            Condition.sleep(Random.nextInt(80, 151))
        }

        if (!Inventory.isFull()) {
            logger.warn("Stopped buying before inventory filled.")
            return
        }
    }

    private fun handleBanking() {
        currentTask = "Depositing wine"
        val wineBefore = wineCount()

        if (wineBefore > 0) {
            val deposited = Bank.deposit().item(JUG_OF_WINE, Quantity.of(Bank.Amount.ALL.value)).submit()
            if (deposited) {
                Condition.wait({ wineCount() == 0 }, 50, 40)
            }
            if (wineCount() > 0) {
                logger.warn("Failed to deposit all wine.")
                return
            }
            inventoryTrips++
        }

        Bank.close()
        Condition.wait({ !Bank.opened() }, 50, 20)
        Condition.sleep(Random.nextInt(120, 220))
    }

    private fun isAtFixedBank(): Boolean {
        return Players.local().tile().distanceTo(FIXED_BANK_TILE) <= FIXED_BANK_RADIUS
    }

    private fun openFixedBank(): Boolean {
        if (Bank.opened()) return true

        val bankObject = Objects.stream()
            .action("Bank")
            .within(FIXED_BANK_TILE, FIXED_BANK_RADIUS)
            .nearest()
            .firstOrNull()

        if (bankObject != null && bankObject.valid() && bankObject.interact("Bank")) {
            return Condition.wait({ Bank.opened() }, 50, 200)
        }

        val banker = Npcs.stream()
            .action("Bank")
            .within(FIXED_BANK_TILE, FIXED_BANK_RADIUS)
            .nearest()
            .firstOrNull()

        if (banker != null && banker.valid() && banker.interact("Bank")) {
            return Condition.wait({ Bank.opened() }, 50, 200)
        }

        return false
    }

    private fun wineCount(): Int {
        return Inventory.stream().name(JUG_OF_WINE).count().toInt()
    }
}


fun main() {
    val script = WineBuyer()
    script.startScript("localhost", "0m6", false)
}
