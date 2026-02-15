package org.powbot.om6.herbrun.services

import org.powbot.api.Condition
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Movement
import org.powbot.om6.api.MovementUtils
import org.powbot.om6.herbrun.HerbRun
import org.powbot.om6.herbrun.config.HerbRunConfig

class PreflightBankingService(private val script: HerbRun) {

    fun performPreflightBanking() {
        // Always execute full preflight banking at run start so each run resets to a consistent loadout.
        MovementUtils.enableRunIfNeeded()
        if (!script.preflightIsNearTile(script.preflightBankTile())) {
            if (Bank.opened()) {
                Bank.close()
            }
            script.preflightInfo("Walking to Farming Guild bank.")
            Movement.walkTo(script.preflightBankTile())
            if (!Condition.wait({ script.preflightIsNearTile(script.preflightBankTile()) }, 300, 25)) {
                script.preflightWarn("Unable to reach Farming Guild bank for preflight banking. Retrying...")
                return
            }
        }

        if (!Bank.opened() && !script.preflightOpenBank("preflight")) {
            val reason = "Unable to open Farming Guild bank for preflight banking."
            script.preflightError(reason)
            script.preflightStop(reason)
            return
        }
        if (!script.preflightIsNearGuildBank()) {
            val reason = "Opened a bank that is not at the Farming Guild tile."
            script.preflightError(reason)
            script.preflightStop(reason)
            return
        }

        script.preflightInfo("Depositing full inventory before preflight withdrawals.")
        if (!script.preflightDepositInventory()) {
            val reason = "Unable to deposit inventory for preflight banking."
            script.preflightError(reason)
            script.preflightStop(reason)
            return
        }

        val missingAfterDeposit = buildPreflightMissingList(script.preflightConfig())
        if (!withdrawPreflightItems(missingAfterDeposit)) {
            val reason = "Missing required supplies/tools in bank: ${missingAfterDeposit.joinToString { it.name }}"
            script.preflightError(reason)
            script.preflightStop(reason)
            return
        }

        Bank.close()
        script.preflightSetComplete()
        script.preflightInfo("Preflight banking complete.")
    }

    private data class PreflightItem(
        val name: String,
        val requiredTotal: Int,
        val useContains: Boolean = true
    )

    private fun buildPreflightMissingList(config: HerbRunConfig): List<PreflightItem> {
        val required = mutableListOf<PreflightItem>()
        val patchCount = config.enabledPatches.size

        required.addAll(buildSeedPreflightItems(config, patchCount))

        val compostName = config.compostItemName
        if (!compostName.isNullOrBlank()) {
            val requiredCompost = if (compostName.contains("bottomless", ignoreCase = true)) 1 else patchCount
            val inv = Inventory.stream().name(compostName).count(true).toInt()
            val needed = (requiredCompost - inv).coerceAtLeast(0)
            if (needed > 0) {
                required.add(PreflightItem(compostName, requiredTotal = requiredCompost, useContains = false))
            }
        }

        val toolNames = listOfNotNull(
            if (script.preflightRequiresRake()) "Rake" else null,
            "Spade",
            if (script.preflightRequiresSeedDibber()) "Seed dibber" else null,
            "Magic secateurs"
        )
        toolNames.forEach { tool ->
            if (Inventory.stream().nameContains(tool).isEmpty()) {
                required.add(PreflightItem(tool, requiredTotal = 1, useContains = true))
            }
        }

        required.addAll(parseAdditionalWithdrawals())

        return required
    }

    private fun buildSeedPreflightItems(config: HerbRunConfig, patchCount: Int): List<PreflightItem> {
        if (patchCount <= 0) {
            return emptyList()
        }

        val seedPriority = config.seedPriorityItemNames
        if (seedPriority.isEmpty()) {
            return emptyList()
        }

        val bankOpened = Bank.opened()
        val items = mutableListOf<PreflightItem>()
        var remaining = patchCount

        for (seedName in seedPriority) {
            if (remaining <= 0) break
            val inv = Inventory.stream().name(seedName).count(true).toInt()
            val bank = if (bankOpened) Bank.stream().name(seedName).count(true).toInt() else 0
            val available = inv + bank
            if (available <= 0) {
                continue
            }
            val assign = remaining.coerceAtMost(available)
            items.add(PreflightItem(seedName, requiredTotal = inv + assign, useContains = false))
            remaining -= assign
        }

        if (remaining > 0) {
            val primarySeed = seedPriority.first()
            val currentPrimary = Inventory.stream().name(primarySeed).count(true).toInt()
            items.add(PreflightItem(primarySeed, requiredTotal = currentPrimary + remaining, useContains = false))
        }

        return items
    }

    private fun parseAdditionalWithdrawals(): List<PreflightItem> {
        val raw = script.preflightAdditionalWithdrawalsRaw()
        if (raw.isBlank()) {
            return emptyList()
        }

        val results = mutableListOf<PreflightItem>()
        val entries = raw.split(",")
        for (entry in entries) {
            val trimmed = entry.trim()
            if (trimmed.isEmpty()) continue
            val parts = trimmed.split(":")
            if (parts.size != 2) {
                script.preflightWarn("Invalid additional withdrawal entry: \"$trimmed\" (use Item:Amount)")
                continue
            }
            val name = parts[0].trim()
            val amount = parts[1].trim().toIntOrNull()
            if (name.isEmpty() || amount == null || amount <= 0) {
                script.preflightWarn("Invalid additional withdrawal entry: \"$trimmed\" (use Item:Amount)")
                continue
            }
            results.add(PreflightItem(name, requiredTotal = amount, useContains = false))
        }

        return results
    }

    private fun withdrawPreflightItems(items: List<PreflightItem>): Boolean {
        for (item in items) {
            val bankItem = if (item.useContains) {
                Bank.stream().nameContains(item.name).firstOrNull()
            } else {
                Bank.stream().name(item.name).firstOrNull()
            }
            if (bankItem == null || !bankItem.valid()) {
                return false
            }
            val before = if (item.useContains) {
                Inventory.stream().nameContains(item.name).count(true).toInt()
            } else {
                Inventory.stream().name(item.name).count(true).toInt()
            }
            val neededNow = (item.requiredTotal - before).coerceAtLeast(0)
            if (neededNow == 0) {
                continue
            }
            script.preflightInfo("Withdrawing ${item.name} x$neededNow")
            val inventoryCheck = {
                if (item.useContains) {
                    Inventory.stream().nameContains(item.name).count(true).toInt()
                } else {
                    Inventory.stream().name(item.name).count(true).toInt()
                }
            }
            if (!script.preflightWithdrawById(bankItem.id(), inventoryCheck, neededNow)) {
                return false
            }
        }
        return true
    }
}
