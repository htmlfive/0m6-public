package org.powbot.community.herblore.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Game
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Skills
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.community.herblore.HerbloreTrainer
import org.powbot.community.herblore.data.HerbDefinition
import org.powbot.community.herblore.data.HerbDefinitions
import kotlin.random.Random

class CleanHerbsTask(script: HerbloreTrainer) : HerbloreTask(script, "Herb cleaning") {
    private val clickedSlots: MutableSet<Int> = mutableSetOf()
    private val zigZagOrder: List<Int> = buildZigZagOrder()
    private val zigZagPriority: Map<Int, Int> = zigZagOrder.withIndex().associate { (priority, slot) -> slot to priority }

    override fun shouldExecute(): Boolean {
        val config = script.runtimeConfig()
        if (config.wholeShowEnabled) {
            return script.wholeShowPhase() == HerbloreTrainer.WholeShowPhase.CLEANING
        }
        if (config.autoCleanAndUnfAll) {
            return hasAnyGrimyHerbInInventory() || hasEligibleGrimyHerbInBank()
        }
        return config.mode.uiValue.equals("Herb cleaning", ignoreCase = true)
    }

    override fun execute() {
        script.setTaskStatus("Cleaning herbs")

        if (hasAnyGrimyHerbInInventory()) {
            script.bankingService.ensureClosed()
            cleanInventoryCycle()
            return
        }

        clickedSlots.clear()

        if (!script.bankingService.ensureOpen()) {
            script.setTaskStatus("Failed to open bank")
            return
        }

        script.bankingService.depositInventory()

        val herb = findTargetHerb() ?: run {
            if (script.runtimeConfig().autoCleanAndUnfAll) {
                script.setTaskStatus("No cleanable grimy herbs available")
                return
            }
            if (script.runtimeConfig().wholeShowEnabled) {
                script.advanceWholeShowToUnfinished()
                return
            }
            script.stopWithNotification("No cleanable grimy herbs available. Stopping script.")
            return
        }

        if (!script.bankingService.withdraw(herb.grimyName, 28)) {
            if (script.runtimeConfig().wholeShowEnabled) {
                script.setTaskStatus("Failed to withdraw ${herb.grimyName}, retrying")
                return
            }
            script.stopWithNotification("Out of ${herb.grimyName}. Stopping script.")
            return
        }

        script.bankingService.ensureClosed()
        clickedSlots.clear()
        cleanInventoryCycle()
    }

    private fun cleanInventoryCycle() {
        ensureInventoryTabOpen()
        val grimyNames = HerbDefinitions.all.map { it.grimyName }.toTypedArray()
        val herbs = Inventory.stream().name(*grimyNames).toList().sortedBy { slotPriority(it.inventoryIndex()) }
        var clickedAny = false
        herbs.forEach { herb ->
            if (!herb.valid()) return@forEach
            val slot = herb.inventoryIndex()
            if (slot < 0 || clickedSlots.contains(slot)) return@forEach
            if (herb.click()) {
                clickedSlots.add(slot)
                clickedAny = true
            }
            Condition.sleep(Random.nextInt(80, 151))
        }
        if (!clickedAny) {
            Condition.sleep(80)
        }
    }

    private fun slotPriority(slotIndex: Int): Int {
        return zigZagPriority[slotIndex] ?: Int.MAX_VALUE
    }

    private fun buildZigZagOrder(): List<Int> {
        val ordered = mutableListOf<Int>()
        for (row in 0 until 7) {
            ordered.add(row * 4)
            ordered.add(row * 4 + 1)
        }
        for (row in 0 until 7) {
            ordered.add(row * 4 + 2)
            ordered.add(row * 4 + 3)
        }
        return ordered
    }

    private fun findTargetHerb(): HerbDefinition? {
        val config = script.runtimeConfig()
        val level = Skills.realLevel(Skill.Herblore)
        val makeAll = config.wholeShowEnabled || config.autoCleanAndUnfAll || config.cleaningMakeAll

        return if (makeAll) {
            HerbDefinitions.all.firstOrNull { level >= it.cleanLevel && script.bankingService.hasInBank(it.grimyName) }
        } else {
            val selected = config.cleaningHerb ?: return null
            if (level < selected.cleanLevel) return null
            if (!script.bankingService.hasInBank(selected.grimyName)) return null
            selected
        }
    }

    private fun hasAnyGrimyHerbInInventory(): Boolean {
        val grimyNames = HerbDefinitions.all.map { it.grimyName }.toTypedArray()
        return Inventory.stream().name(*grimyNames).isNotEmpty()
    }

    private fun hasEligibleGrimyHerbInBank(): Boolean {
        val level = Skills.realLevel(Skill.Herblore)
        return HerbDefinitions.all.any { level >= it.cleanLevel && script.bankingService.hasInBank(it.grimyName) }
    }

    private fun ensureInventoryTabOpen() {
        if (Game.tab() == Game.Tab.INVENTORY) return
        Game.tab(Game.Tab.INVENTORY)
        Condition.wait({ Game.tab() == Game.Tab.INVENTORY }, 80, 10)
    }
}

