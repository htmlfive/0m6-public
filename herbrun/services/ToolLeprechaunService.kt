package org.powbot.om6.herbrun.services

import org.powbot.api.Condition
import org.powbot.api.rt4.Components
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Item
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Npc
import org.powbot.api.rt4.Npcs
import org.powbot.api.rt4.Players
import org.powbot.om6.api.MovementUtils
import org.powbot.om6.herbrun.HerbRun
import org.powbot.om6.herbrun.config.HerbPatch
import kotlin.random.Random

class ToolLeprechaunService(private val script: HerbRun) {

    fun ensureToolsAtPatch(patch: HerbPatch): Boolean {
        val desiredCompost = script.toolDesiredCompostName()
        val missingTools = listOfNotNull(
            if (script.toolRequiresRake()) "Rake" else null,
            "Spade",
            if (script.toolRequiresSeedDibber()) "Seed dibber" else null,
            "Magic secateurs"
        )
            .filter { Inventory.stream().name(it).isEmpty() }
            .toMutableList()
        if (!desiredCompost.isNullOrBlank() && Inventory.stream().name(desiredCompost).isEmpty()) {
            missingTools.add(desiredCompost)
        }
        if (missingTools.isEmpty()) {
            return true
        }

        val leprechaun =
            Npcs.stream().name("Tool Leprechaun").within(patch.tile, 14.0).nearest().firstOrNull()
                ?: run {
                    script.toolWarn("${patch.displayName}: Tool Leprechaun not found for tool exchange.")
                    return false
                }

        if (!openLeprechaunExchange(leprechaun)) {
            script.toolWarn("${patch.displayName}: Failed to open Tool Leprechaun exchange.")
            return false
        }

        missingTools.forEach { toolName ->
            if (Inventory.stream().name(toolName).isEmpty()) {
                script.toolInfo("${patch.displayName}: Withdrawing tool $toolName from exchange.")
                withdrawFromExchange(toolName)
                Condition.sleep(Random.nextInt(200, 350))
            }
        }

        val stillMissing = missingTools.any { Inventory.stream().name(it).isEmpty() }
        if (stillMissing) {
            script.toolWarn("${patch.displayName}: Tool exchange did not provide all tools: ${missingTools.joinToString()}")
        }
        return !stillMissing
    }

    fun noteHerbs(patch: HerbPatch) {
        val herbItem = findHerbProduceItemInInventory()
        if (herbItem == null) {
            script.toolWarn("${patch.displayName}: No herb item found to note.")
            return
        }
        val leprechaun = findToolLeprechaunForPatch(patch)
        if (leprechaun == null || !leprechaun.valid()) {
            script.toolWarn("${patch.displayName}: Tool Leprechaun not found for noting.")
            return
        }

        if (Players.local().tile().distanceTo(leprechaun.tile()) > 6.0) {
            script.toolInfo("${patch.displayName}: Walking closer to Tool Leprechaun for noting.")
            MovementUtils.enableRunIfNeeded()
            Movement.walkTo(leprechaun.tile())
            Condition.wait({ Players.local().tile().distanceTo(leprechaun.tile()) <= 6.0 }, 250, 16)
        }

        val before = countHerbProduceInInventory()
        script.toolInfo("${patch.displayName}: Using herb on Tool Leprechaun.")
        if (!herbItem.interact("Use")) {
            script.toolWarn("${patch.displayName}: Failed to use herb item before noting.")
            return
        }
        Condition.sleep(Random.nextInt(180, 320))
        script.toolInfo("${patch.displayName}: Noting herbs.")
        if (!leprechaun.interact("Use") && !leprechaun.interact("Exchange")) {
            script.toolWarn("${patch.displayName}: Failed to interact with Tool Leprechaun for noting.")
            return
        }

        Condition.wait(
            { countHerbProduceInInventory() < before },
            400,
            15
        )
    }

    private fun findToolLeprechaunForPatch(patch: HerbPatch): Npc? {
        val byPatch = Npcs.stream()
            .name("Tool Leprechaun")
            .within(patch.tile, 22.0)
            .nearest()
            .firstOrNull()
        if (byPatch != null && byPatch.valid()) {
            return byPatch
        }
        return Npcs.stream()
            .name("Tool Leprechaun")
            .within(Players.local().tile(), 22.0)
            .nearest()
            .firstOrNull()
    }

    private fun findHerbProduceItemInInventory(): Item? {
        return Inventory.stream()
            .filtered { isCleanableHerb(it) }
            .firstOrNull()
    }

    private fun countHerbProduceInInventory(): Int {
        return Inventory.stream()
            .filtered { isCleanableHerb(it) }
            .count(true)
            .toInt()
    }

    private fun isCleanableHerb(item: Item): Boolean {
        val actions = item.actions()
        return actions.any { it.equals("Clean", ignoreCase = true) }
    }

    private fun openLeprechaunExchange(leprechaun: Npc): Boolean {
        if (isExchangeOpen()) {
            return true
        }
        script.toolInfo("Opening Tool Leprechaun exchange.")
        if (!leprechaun.interact("Exchange")) {
            return false
        }
        return Condition.wait({ isExchangeOpen() }, 300, 15)
    }

    private fun isExchangeOpen(): Boolean {
        return Components.stream().textContains("Tool Leprechaun", "Exchange").isNotEmpty() ||
            Components.stream().filtered {
                val name = it.name()
                name.contains("Rake", ignoreCase = true) ||
                    name.contains("Seed dibber", ignoreCase = true) ||
                    name.contains("Spade", ignoreCase = true) ||
                    name.contains("Magic secateurs", ignoreCase = true)
            }.isNotEmpty()
    }

    private fun withdrawFromExchange(itemName: String): Boolean {
        val component = Components.stream()
            .filtered { it.name().contains(itemName, ignoreCase = true) }
            .firstOrNull()
            ?: return false
        if (!component.visible()) return false
        val action = component.actions()
            .firstOrNull { it.contains("Withdraw", ignoreCase = true) || it.contains("Take", ignoreCase = true) }
            ?: return component.click()
        return component.interact(action)
    }
}
