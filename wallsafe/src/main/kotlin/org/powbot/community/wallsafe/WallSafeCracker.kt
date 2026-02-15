package org.powbot.community.wallsafe

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.script.AbstractScript
import org.powbot.api.script.ScriptCategory
import org.powbot.api.script.ScriptConfiguration
import org.powbot.api.script.ScriptManifest
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Combat
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Npcs
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Skills
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.community.api.ScriptLogging

@ScriptManifest(
    name = "0m6 Wall Safe Cracker",
    description = "Cracks wall safe, drinks wine at 20-30 HP, banks loot, and restocks wine.",
    version = "1.0.1",
    author = "0m6",
    category = ScriptCategory.Thieving
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            "Info",
            "Start near a wall safe. Safe tile 3055,4970,1 and bank tile 3043,4972,1. Script does not webwalk.",
            optionType = org.powbot.api.script.OptionType.INFO
        ),
        ScriptConfiguration(
            "Wine Withdraw Amount",
            "How many Jug of wine to withdraw when banking.",
            optionType = org.powbot.api.script.OptionType.INTEGER,
            defaultValue = "10"
        )
    ]
)
class WallSafeCracker : AbstractScript() {

    private companion object {
        const val WALL_SAFE_NAME = "Wall safe"

        const val WINE_NAME = "Jug of wine"
        const val BANKER_NAME = "Emerald Benedict"
        val BANK_TILE = Tile(3043, 4972, 1)
        val SAFE_TILE = Tile(3055, 4970, 1)

        val DEPOSIT_TARGETS = listOf(
            "Jug",
            "Jug of wine",
            "Sapphire",
            "Emerald",
            "Ruby",
            "Diamond",
            "Uncut sapphire",
            "Uncut emerald",
            "Uncut ruby",
            "Uncut diamond"
        )
    }

    private var currentTask = "Initializing"
    private var safesCracked = 0
    private var trips = 0
    private var wineDrunk = 0
    private var safeTile: Tile = SAFE_TILE
    private var wineWithdrawAmount = 10

    override fun onStart() {
        wineWithdrawAmount = getOption<Int>("Wine Withdraw Amount").coerceAtLeast(1)

        addPaint(
            PaintBuilder.newBuilder()
                .x(40).y(80)
                .addString("Task") { currentTask }
                .addString("Safes Cracked") { safesCracked.toString() }
                .trackInventoryItems(1617, 1621, 1623, 1619)
                .trackSkill(Skill.Thieving)
                .build()
        )
    }

    override fun poll() {
        if (shouldDrinkWine()) {
            currentTask = "Drinking wine"
            drinkWine()
            return
        }

        if (shouldBank()) {
            handleBanking()
            return
        }

        crackWallSafe()
    }

    private fun shouldDrinkWine(): Boolean {
        val hp = Combat.health()
        return hp in 20..30 && wineCount() > 0
    }

    private fun shouldBank(): Boolean {
        return Inventory.isFull() || wineCount() == 0
    }

    private fun handleBanking() {
        ScriptLogging.action(
            logger,
            "Banking intent: invFull=${Inventory.isFull()}, wineCount=${wineCount()}, withdrawTarget=$wineWithdrawAmount"
        )
        if (Bank.opened()) {
            currentTask = "Banking"
            performBanking()
            return
        }

        if (Players.local().tile().distanceTo(BANK_TILE) > 4) {
            currentTask = "Walking to bank"
            stepToward(BANK_TILE)
            return
        }

        currentTask = "Opening bank"
        openBankAtFixedTile()
    }

    private fun performBanking() {
        ScriptLogging.action(logger, "Banking start")
        depositTargets()

        val currentWine = wineCount()
        if (currentWine < wineWithdrawAmount) {
            val needed = wineWithdrawAmount - currentWine
            currentTask = "Withdrawing wine"
            ScriptLogging.action(logger, "Withdrawing $needed x $WINE_NAME (current=$currentWine, target=$wineWithdrawAmount)")
            if (Bank.withdraw(WINE_NAME, needed)) {
                Condition.wait({ wineCount() >= wineWithdrawAmount }, 120, 20)
                ScriptLogging.action(logger, "Withdraw result: wineCount=${wineCount()}")
            } else {
                ScriptLogging.warn(logger, "Withdraw failed for $WINE_NAME x$needed")
            }
        }

        if (wineCount() == 0) {
            ScriptLogging.stopWithNotification(this, "No Jug of wine available after banking. Stopping script.")
            return
        }

        trips++
        ScriptLogging.action(logger, "Banking complete: trips=$trips, wineCount=${wineCount()}")
        Bank.close()
        Condition.wait({ !Bank.opened() }, 100, 15)
    }

    private fun depositTargets() {
        for (name in DEPOSIT_TARGETS) {
            val count = Inventory.stream().name(name).count().toInt()
            if (count <= 0) continue
            ScriptLogging.action(logger, "Depositing $count x $name")
            Bank.deposit(name, count)
            Condition.wait({ Inventory.stream().name(name).isEmpty() }, 100, 12)
            val remaining = Inventory.stream().name(name).count().toInt()
            if (remaining > 0) {
                ScriptLogging.warn(logger, "Deposit partial/failed for $name, remaining=$remaining")
            }
        }
    }

    private fun crackWallSafe() {
        if (shouldDrinkWine()) {
            currentTask = "Drinking wine"
            drinkWine()
            return
        }

        if (Players.local().tile().distanceTo(safeTile) > 4) {
            currentTask = "Walking to wall safe"
            stepToward(safeTile)
            return
        }

        val safe = Objects.stream()
            .name(WALL_SAFE_NAME)
            .within(safeTile, 6.0)
            .nearest()
            .firstOrNull()

        if (safe == null || !safe.valid()) {
            currentTask = "Waiting for wall safe"
            Condition.sleep(250)
            return
        }

        currentTask = "Cracking wall safe"
        val startingXp = Skills.experience(Skill.Thieving)
        val startingHp = Combat.health()
        if (safe.click()) {
            val animationStarted = Condition.wait({ Players.local().animation() != -1 }, 120, 20)
            if (!animationStarted) {
                return
            }

            val gainedThievingXp = Condition.wait({
                Skills.experience(Skill.Thieving) > startingXp || Combat.health() < startingHp
            }, 150, 24)
            if (gainedThievingXp) {
                if (Skills.experience(Skill.Thieving) > startingXp) {
                    safesCracked++
                    return
                }
                if (Combat.health() < startingHp) {
                    currentTask = "Took damage - refinding safe"
                    refindNearestSafeAndReclick()
                }
            }
        }
    }

    private fun refindNearestSafeAndReclick() {
        if (shouldDrinkWine()) {
            currentTask = "Drinking wine"
            drinkWine()
            return
        }

        val nearestSafe = Objects.stream()
            .name(WALL_SAFE_NAME)
            .nearest()
            .firstOrNull()

        if (nearestSafe == null || !nearestSafe.valid()) {
            return
        }

        safeTile = nearestSafe.tile()

        if (Players.local().tile().distanceTo(safeTile) > 4) {
            stepToward(safeTile)
            return
        }

        if (Players.local().animation() == -1) {
            nearestSafe.click()
        }
    }

    private fun drinkWine() {
        if (Bank.opened()) {
            Bank.close()
            Condition.wait({ !Bank.opened() }, 100, 10)
            return
        }

        val wine = Inventory.stream().name(WINE_NAME).firstOrNull()
        if (wine == null || !wine.valid()) return

        val before = Combat.health()
        val beforeWineCount = wineCount()
        if (wine.click()) {
            val drank = Condition.wait({ Combat.health() > before || wineCount() < beforeWineCount }, 150, 12)
            if (drank) {
                wineDrunk++
            }
        }
    }

    private fun openBankAtFixedTile(): Boolean {
        if (Bank.opened()) return true

        val banker = Npcs.stream()
            .name(BANKER_NAME)
            .action("Bank")
            .within(BANK_TILE, 10.0)
            .nearest()
            .firstOrNull()

        if (banker != null && banker.valid() && banker.interact("Bank")) {
            return Condition.wait({ Bank.opened() }, 120, 30)
        }

        val bankObject = Objects.stream()
            .action("Bank")
            .within(BANK_TILE, 10.0)
            .nearest()
            .firstOrNull()

        if (bankObject != null && bankObject.valid() && bankObject.interact("Bank")) {
            return Condition.wait({ Bank.opened() }, 120, 30)
        }

        return false
    }

    private fun stepToward(tile: Tile) {
        if (Players.local().inMotion()) return
        val distance = Players.local().tile().distanceTo(tile)
        if (distance <= 2) return
        val stepped = Movement.step(tile)
        if (stepped) {
            Condition.wait({ Players.local().inMotion() || Players.local().tile().distanceTo(tile) < distance }, 120, 10)
        } else {
            Condition.sleep(220)
        }
    }

    private fun wineCount(): Int {
        return Inventory.stream().name(WINE_NAME).count().toInt()
    }
}


fun main() {
    val script = WallSafeCracker()
    script.startScript("localhost", "0m6", false)
}

