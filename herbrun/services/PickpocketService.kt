package org.powbot.om6.herbrun.services

import org.powbot.api.Condition
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Combat
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Npcs
import org.powbot.api.rt4.Players
import org.powbot.om6.api.MovementUtils
import org.powbot.om6.herbrun.HerbRun
import kotlin.random.Random

private const val MASTER_FARMER_NAME = "Master Farmer"
private const val PICKPOCKET_WINE_NAME = "Jug of wine"
private const val STUN_PICKPOCKET_COOLDOWN_MIN_MS = 2400L
private const val STUN_PICKPOCKET_COOLDOWN_MAX_MS = 3000L
private const val PICKPOCKET_TAP_MIN_DELAY_MS = 90L
private const val PICKPOCKET_TAP_MAX_DELAY_MS = 200L

class PickpocketService(private val script: HerbRun) {
    fun performBetweenRunPickpocketing() {
        val now = System.currentTimeMillis()
        script.pickpocketDebug(
            "Pickpocket tick: anim=${Players.local().animation()} moving=${Players.local().inMotion()} " +
                "invFull=${Inventory.isFull()} wine=${countPickpocketWine()} " +
                "blockedFor=${(script.pickpocketBlockedUntilMillis() - now).coerceAtLeast(0L)}ms"
        )

        if (Players.local().animation() == 415) {
            val stunLockout = randomStunCooldownMillis()
            script.pickpocketSetBlockedUntilMillis(System.currentTimeMillis() + stunLockout)
            script.pickpocketDebug("Detected stun animation; lockout refreshed for ${stunLockout}ms")
        }

        if (script.pickpocketSetupPending()) {
            script.pickpocketSetCurrentTask("Pickpocket startup banking")
            script.pickpocketDebug("Startup pickpocket setup pending")
            performPickpocketBanking(forceSetup = true)
            return
        }

        if (script.pickpocketEnableLimpwurtFarming()) {
            script.pickpocketSetLastLimpwurtCheckAtMillis(now)
            if (script.pickpocketHandleLimpwurt()) {
                return
            }
        }

        if (System.currentTimeMillis() < script.pickpocketBlockedUntilMillis()) {
            script.pickpocketSetCurrentTask("Stunned")
            script.pickpocketDebug("Inside stun lockout window")
            if (shouldDrinkWineWhileStunned()) {
                script.pickpocketDebug("Stunned: drinking wine")
                drinkPickpocketWine()
                return
            }
            val dropped = dropPickpocketItemsWhileStunned()
            if (dropped) {
                script.pickpocketDebug("Stunned: dropped configured pickpocket items")
            }
            Condition.sleep(Random.nextInt(160, 260))
            return
        }

        if (shouldDrinkWineWhileStunned()) {
            script.pickpocketSetCurrentTask("Drinking wine (stunned)")
            drinkPickpocketWine()
            return
        }

        if (dropPickpocketItemsWhileStunned()) {
            script.pickpocketSetCurrentTask("Dropping pickpocket loot")
            return
        }

        if (Inventory.isFull() || countPickpocketWine() <= 0) {
            if (Players.local().animation() == 415) {
                script.pickpocketSetCurrentTask("Waiting for stun to end")
                script.pickpocketDebug("Banking needed but stun animation still active")
                Condition.sleep(Random.nextInt(180, 320))
                return
            }
            script.pickpocketSetCurrentTask("Pickpocket banking")
            script.pickpocketInfo("Pickpocket banking needed: invFull=${Inventory.isFull()} wine=${countPickpocketWine()}")
            performPickpocketBanking(forceSetup = false)
            return
        }

        val farmerTile = script.pickpocketMasterFarmerTile()
        if (!script.pickpocketIsNearTile(farmerTile)) {
            script.pickpocketSetCurrentTask("Walking to Master Farmer")
            script.pickpocketDebug("Walking to Master Farmer tile $farmerTile")
            MovementUtils.enableRunIfNeeded()
            Movement.walkTo(farmerTile)
            Condition.wait({ script.pickpocketIsNearTile(farmerTile) }, 300, 20)
            return
        }

        val farmer = Npcs.stream()
            .name(MASTER_FARMER_NAME)
            .within(farmerTile, 12.0)
            .nearest()
            .firstOrNull()
        if (farmer == null || !farmer.valid()) {
            script.pickpocketSetCurrentTask("Waiting for Master Farmer")
            script.pickpocketDebug("Master Farmer not found near $farmerTile")
            Condition.sleep(Random.nextInt(220, 360))
            return
        }

        script.pickpocketSetCurrentTask("Pickpocketing Master Farmer")
        if (Bank.opened()) {
            Bank.close()
            Condition.wait({ !Bank.opened() }, 120, 12)
            return
        }
        val local = Players.local()
        if (local.animation() == 415) {
            script.pickpocketSetBlockedUntilMillis(System.currentTimeMillis() + randomStunCooldownMillis())
            script.pickpocketSetCurrentTask("Stunned")
            script.pickpocketDebug("Stunned before click; lockout refreshed")
            Condition.sleep(Random.nextInt(180, 320))
            return
        }

        if (System.currentTimeMillis() < script.pickpocketBlockedUntilMillis()) {
            script.pickpocketSetCurrentTask("Stunned")
            script.pickpocketDebug("Still inside lockout before click")
            Condition.sleep(Random.nextInt(160, 260))
            return
        }

        if (local.animation() != -1 && local.animation() != 881) {
            script.pickpocketDebug("Skipping click due to animation=${local.animation()}")
            Condition.sleep(Random.nextInt(120, 220))
            return
        }
        if (local.inMotion()) {
            script.pickpocketDebug("Skipping click while moving")
            Condition.sleep(Random.nextInt(120, 220))
            return
        }

        val tapNow = System.currentTimeMillis()
        if (tapNow < script.pickpocketNextTapAtMillis()) {
            Condition.sleep(Random.nextInt(90, 160))
            return
        }

        val clicked = farmer.click()
        val started = Condition.wait(
            {
                val animation = Players.local().animation()
                animation == 415 || animation != -1 || Players.local().inMotion()
            },
            100,
            8
        )
        script.pickpocketSetNextTapAtMillis(tapNow + randomPickpocketTapDelayMillis())
        script.pickpocketDebug("Pickpocket click: clicked=$clicked started=$started animNow=${Players.local().animation()}")
    }

    private fun performPickpocketBanking(forceSetup: Boolean) {
        script.pickpocketInfo("Pickpocket banking step: forceSetup=$forceSetup invCount=${Inventory.stream().count()} wine=${countPickpocketWine()}")
        if (!script.pickpocketIsNearTile(script.pickpocketBankTile())) {
            if (Bank.opened()) {
                Bank.close()
            }
            MovementUtils.enableRunIfNeeded()
            script.pickpocketInfo("Walking to Farming Guild bank for pickpocket supplies.")
            Movement.walkTo(script.pickpocketBankTile())
            Condition.wait({ script.pickpocketIsNearTile(script.pickpocketBankTile()) }, 300, 25)
            return
        }

        if (!Bank.opened()) {
            if (!script.pickpocketOpenBank("pickpocket supplies")) {
                script.pickpocketWarn("Unable to open Farming Guild bank for pickpocket supplies after retries.")
                return
            }
            script.pickpocketInfo("Opened Farming Guild bank for pickpocket supplies")
            return
        }

        if (!script.pickpocketIsNearGuildBank()) {
            script.pickpocketWarn("Opened bank is not at Farming Guild while pickpocketing.")
            Bank.close()
            return
        }

        if (forceSetup || Inventory.stream().isNotEmpty()) {
            if (!script.pickpocketDepositInventory()) {
                script.pickpocketWarn("Failed to deposit inventory for pickpocket supplies after retries.")
                return
            }
            script.pickpocketInfo("Deposited inventory for pickpocket supplies")
        }

        val withdrawAmount = script.pickpocketWineWithdrawAmount()
        if (forceSetup || countPickpocketWine() < withdrawAmount) {
            if (!script.pickpocketWithdrawByName(PICKPOCKET_WINE_NAME, withdrawAmount)) {
                val reason = "No $PICKPOCKET_WINE_NAME available for between-run pickpocketing."
                script.pickpocketError(reason)
                script.pickpocketStop(reason)
                return
            }
            Condition.wait({ countPickpocketWine() >= withdrawAmount }, 120, 20)
            script.pickpocketInfo("Withdrew $PICKPOCKET_WINE_NAME x$withdrawAmount")
        }

        Bank.close()
        Condition.wait({ !Bank.opened() }, 120, 12)
        if (forceSetup) {
            script.pickpocketSetSetupPending(false)
        }
    }

    private fun shouldDrinkWineWhileStunned(): Boolean {
        val local = Players.local()
        if (local.animation() != 415) {
            return false
        }
        return Combat.health() <= (Combat.maxHealth() - script.pickpocketHealHpDeficit()) && countPickpocketWine() > 0
    }

    private fun drinkPickpocketWine() {
        val wine = Inventory.stream().name(PICKPOCKET_WINE_NAME).firstOrNull() ?: return
        val beforeHp = Combat.health()
        val beforeWineCount = countPickpocketWine()
        script.pickpocketDebug("Drink wine attempt: hp=$beforeHp wineCount=$beforeWineCount")
        if (!wine.interact("Drink")) {
            script.pickpocketDebug("Drink wine interact failed")
            return
        }
        val drank = Condition.wait(
            { Combat.health() > beforeHp || countPickpocketWine() < beforeWineCount },
            120,
            15
        )
        script.pickpocketDebug("Drink wine result: success=$drank hpNow=${Combat.health()} wineNow=${countPickpocketWine()}")
    }

    private fun countPickpocketWine(): Int {
        return Inventory.stream().name(PICKPOCKET_WINE_NAME).count(true).toInt()
    }

    private fun randomStunCooldownMillis(): Long {
        return Random.nextLong(STUN_PICKPOCKET_COOLDOWN_MIN_MS, STUN_PICKPOCKET_COOLDOWN_MAX_MS + 1L)
    }

    private fun randomPickpocketTapDelayMillis(): Long {
        return Random.nextLong(PICKPOCKET_TAP_MIN_DELAY_MS, PICKPOCKET_TAP_MAX_DELAY_MS + 1L)
    }

    private fun dropPickpocketItemsWhileStunned(): Boolean {
        if (System.currentTimeMillis() >= script.pickpocketBlockedUntilMillis()) {
            return false
        }
        val dropItems = script.pickpocketDropItems()
        if (dropItems.isEmpty()) {
            return false
        }
        var droppedAny = false
        while (System.currentTimeMillis() < script.pickpocketBlockedUntilMillis()) {
            val item = Inventory.stream().name(*dropItems).firstOrNull() ?: break
            val itemName = item.name()
            val beforeCount = Inventory.stream().name(itemName).count(true).toInt()
            if (!item.interact("Drop")) {
                script.pickpocketDebug("Failed dropping pickpocket item: $itemName")
                break
            }
            droppedAny = true
            script.pickpocketInfo("Dropped pickpocket item: $itemName")
            Condition.wait(
                { Inventory.stream().name(itemName).count(true).toInt() < beforeCount },
                120,
                10
            )
        }
        return droppedAny
    }
}
