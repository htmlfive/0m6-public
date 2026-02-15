package org.powbot.community.api

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Components
import org.powbot.api.rt4.Equipment
import org.powbot.api.rt4.Game
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Item
import org.powbot.api.rt4.Magic
import org.powbot.api.rt4.Npc
import org.powbot.api.rt4.Players
import org.powbot.api.waiter.TickWaiter

object ConsumableUtils {
    private val EQUIPMENT_TELEPORT_ITEMS = setOf("mythical cape")
    private const val TELEPORT_TO_HOUSE_NAME = "Teleport to house"
    private const val ARDOUGNE_CLOAK_NAME = "Ardougne cloak"
    private const val ARDOUGNE_CLOAK_TELEPORT_ACTION = "Monastery Teleport"
    private val POTION_DOSE_PATTERN = Regex("\\((\\d)\\)")
    private val ANTIFIRE_POTIONS = arrayOf(
        "Antifire potion(4)", "Antifire potion(3)", "Antifire potion(2)", "Antifire potion(1)",
        "Extended antifire(4)", "Extended antifire(3)", "Extended antifire(2)", "Extended antifire(1)",
        "Super antifire potion(4)", "Super antifire potion(3)", "Super antifire potion(2)", "Super antifire potion(1)",
        "Extended super antifire(4)", "Extended super antifire(3)", "Extended super antifire(2)", "Extended super antifire(1)"
    )
    
    // Configurable secondary food options - can be extended with more foods
    private val secondaryFoodOptions = mutableListOf("Bass", "Lobster", "Swordfish", "Tuna", "Trout", "Salmon")

    fun getFood(foodName: String): Item {
        // Try configured food first
        val configuredFood = Inventory.stream().name(foodName).firstOrNull()
        if (configuredFood?.valid() == true) {
            return configuredFood
        }
        
        // Try secondary food options in order
        for (secondaryFood in secondaryFoodOptions) {
            val food = Inventory.stream().name(secondaryFood).firstOrNull()
            if (food?.valid() == true) {
                return food
            }
        }
        
        return Item.Nil
    }

    fun getFoodWithOptions(primaryFood: String, secondaryOptions: List<String> = secondaryFoodOptions): Item {
        // Try primary food first
        val primary = Inventory.stream().name(primaryFood).firstOrNull()
        if (primary?.valid() == true) {
            return primary
        }
        
        // Try secondary options in order
        for (option in secondaryOptions) {
            val secondary = Inventory.stream().name(option).firstOrNull()
            if (secondary?.valid() == true) {
                return secondary
            }
        }
        
        return Item.Nil
    }

    fun getFoodWithPriority(priorityList: List<String>): Item {
        for (foodName in priorityList) {
            val food = Inventory.stream().name(foodName).firstOrNull()
            if (food?.valid() == true) {
                return food
            }
        }
        return Item.Nil
    }

    fun eatFood(food: Item): Boolean {
        if (!food.valid()) return false
        return food.interact("Eat")
    }

    fun getAntipoison(): Item {
        val antipoisons = arrayOf(
            "Antipoison(4)", "Antipoison(3)", "Antipoison(2)", "Antipoison(1)",
            "Superantipoison(4)", "Superantipoison(3)", "Superantipoison(2)", "Superantipoison(1)",
            "Antidote+(4)", "Antidote+(3)", "Antidote+(2)", "Antidote+(1)",
            "Antidote++(4)", "Antidote++(3)", "Antidote++(2)", "Antidote++(1)"
        )
        return Inventory.stream().name(*antipoisons).first()
    }

    fun drinkAntipoison(antipoison: Item): Boolean {
        if (!antipoison.valid()) return false
        Inventory.open()
        Condition.sleep(Random.nextInt(90, 120))
        return antipoison.click()
    }

    fun getAntifirePotion(): Item =
        antifirePotionStream()
            .toList()
            .sortedWith(compareBy({ potionDose(it) }, { it.name() }))
            .firstOrNull() ?: Item.Nil

    fun drinkAntifirePotion(potion: Item): Boolean {
        if (!potion.valid()) return false
        Inventory.open()
        Condition.sleep(Random.nextInt(90, 120))
        return potion.interact("Drink")
    }

    fun isOutOfAntifirePotion(): Boolean =
        antifirePotionStream().isEmpty()

    fun countAntifirePotions(): Int =
        antifirePotionStream().count(true).toInt()

    fun getStaminaPotion(): Item =
        Inventory.stream().nameContains("Stamina potion").first()

    fun drinkStaminaPotion(potion: Item): Boolean {
        if (!potion.valid()) return false
        Inventory.open()
        Condition.sleep(Random.nextInt(90, 120))
        return potion.interact("Drink")
    }

    fun isOutOfStaminaPotion(): Boolean =
        Inventory.stream().nameContains("Stamina potion").isEmpty()

    fun isOutOfFood(foodName: String): Boolean {
        // Check if completely out of food (primary + all secondary options)
        val allFoodNames = listOf(foodName) + secondaryFoodOptions
        return Inventory.stream().name(*allFoodNames.toTypedArray()).isEmpty()
    }

    fun isOutOfFoodWithOptions(primaryFood: String, secondaryOptions: List<String> = secondaryFoodOptions): Boolean {
        val allFoodNames = listOf(primaryFood) + secondaryOptions
        return Inventory.stream().name(*allFoodNames.toTypedArray()).isEmpty()
    }

    fun hasAnyFood(primaryFood: String, secondaryOptions: List<String> = secondaryFoodOptions): Boolean {
        val allFoodNames = listOf(primaryFood) + secondaryOptions
        return Inventory.stream().name(*allFoodNames.toTypedArray()).isNotEmpty()
    }

    fun hasAnyFoodFromList(foodList: List<String>): Boolean {
        return Inventory.stream().name(*foodList.toTypedArray()).isNotEmpty()
    }

    // Add new secondary food options dynamically
    fun addSecondaryFood(foodName: String) {
        if (foodName !in secondaryFoodOptions) {
            secondaryFoodOptions.add(foodName)
        }
    }

    // Remove a secondary food option
    fun removeSecondaryFood(foodName: String) {
        secondaryFoodOptions.remove(foodName)
    }

    // Get all configured secondary food options
    fun getSecondaryFoodOptions(): List<String> = secondaryFoodOptions.toList()

    fun isOutOfAntipoison(): Boolean =
        Inventory.stream().nameContains("Antipoison", "antipoison").isEmpty()

    fun isOutOfWaterskin(): Boolean {
        val waterskins = Inventory.stream().nameContains("Waterskin").toList()
        return waterskins.isEmpty() || waterskins.all { it.name() == "Waterskin(0)" }
    }

    fun isOutOfGamesNecklace(): Boolean =
        Inventory.stream().nameContains("Games necklace").isEmpty()

    fun isOutOfRingOfDueling(): Boolean =
        Inventory.stream()
            .nameContains("Ring of dueling")
            .isEmpty()

    fun isOutOfIceCooler(): Boolean =
        Inventory.stream().name("Ice cooler").isEmpty()

    fun getIceCooler(): Item =
        Inventory.stream().name("Ice cooler").first()

    fun useIceCoolerOnNpc(npc: Npc): Boolean {
        val iceCooler = getIceCooler()
        if (!iceCooler.valid()) return false
        if (iceCooler.click()) {
            TickWaiter(1).wait()
            return npc.interact("Use")
        }
        return false
    }

    fun hasRockHammer(): Boolean =
        Inventory.stream().name("Rock hammer").isNotEmpty()

    fun getRockHammer(): Item =
        Inventory.stream().name("Rock hammer").first()

    fun useRockHammerOnNpc(npc: Npc): Boolean {
        val hammer = getRockHammer()
        if (!hammer.valid()) return false
        if (hammer.click()) {
            TickWaiter(1).wait()
            return npc.interact("Use")
        }
        return false
    }

    fun hasFungicideSpray(): Boolean =
        fungicideSprayStream().filtered(::hasFungicideCharges).isNotEmpty()

    fun countFungicideSpraysWithCharges(): Int =
        fungicideSprayStream()
            .toList()
            .count(::hasFungicideCharges)

    fun getFungicideSpray(): Item =
        fungicideSprayStream()
            .map { it to extractFungicideCharges(it) }
            .filter { (_, charges) -> charges != null && charges > 0 }
            .minByOrNull { it.second!! }
            ?.first ?: fungicideSprayStream().filtered(::hasFungicideCharges).first()

    fun useFungicideSprayOnNpc(npc: Npc): Boolean {
        val spray = getFungicideSpray()
        if (!spray.valid()) return false
        if (spray.click()) {
            TickWaiter(1).wait()
            return npc.interact("Use")
        }
        return false
    }

    private fun fungicideSprayStream() =
        Inventory.stream().nameContains("Fungicide spray")

    private fun hasFungicideCharges(item: Item): Boolean {
        val charges = extractFungicideCharges(item)
        return charges == null || charges > 0
    }

    private fun extractFungicideCharges(item: Item): Int? {
        if (!item.valid()) {
            return null
        }
        val name = item.name()
        val suffix = name.substringAfterLast(' ', "")
        return suffix.toIntOrNull()
    }

    private fun antifirePotionStream() =
        Inventory.stream().name(*ANTIFIRE_POTIONS)

    private fun potionDose(item: Item): Int =
        POTION_DOSE_PATTERN.find(item.name())?.groupValues?.getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE

    fun hasEctophial(): Boolean =
        Inventory.stream().nameContains("Ectophial").isNotEmpty()

    fun useEctophial(): Boolean {
        val ectophial = Inventory.stream().nameContains("Ectophial").first()
        if (!ectophial.valid()) return false
        if (ectophial.interact("Empty")) {
            return Condition.wait({ Players.local().animation() != -1 }, 100, 30)
        }
        return false
    }

    fun hasEmergencyTeleportItem(itemName: String): Boolean {
        val trimmedName = itemName.trim()
        if (trimmedName.isEmpty()) {
            return false
        }
        if (isTeleportToHouseSpell(trimmedName)) {
            return Magic.Spell.TELEPORT_TO_HOUSE.canCast()
        }
        if (Inventory.stream().nameContains(trimmedName).isNotEmpty()) {
            return true
        }
        if (isArdougneCloak(trimmedName) && Equipment.stream().nameContains(ARDOUGNE_CLOAK_NAME).isNotEmpty()) {
            return true
        }
        return isEquipmentTeleportItem(trimmedName) &&
            Equipment.stream().nameContains(trimmedName).isNotEmpty()
    }

    fun useEmergencyTeleportItem(itemName: String): Boolean {
        val trimmedName = itemName.trim()
        if (trimmedName.isEmpty()) {
            return false
        }
        if (isTeleportToHouseSpell(trimmedName)) {
            return castTeleportToHouse()
        }
        if (isArdougneCloak(trimmedName)) {
            if (useArdougneCloakTeleportFromInventory()) {
                return true
            }
            if (useArdougneCloakTeleportFromEquipment()) {
                return true
            }
        }

        val hasEquipmentTeleport = isEquipmentTeleportItem(trimmedName) &&
            Equipment.stream().nameContains(trimmedName).isNotEmpty()
        if (hasEquipmentTeleport && useEquipmentTeleport(trimmedName)) {
            return true
        }

        val teleportItem = Inventory.stream().nameContains(trimmedName).first()
        if (!teleportItem.valid()) return false
        if (isMythicalCape(trimmedName)) {
            return useMythicalCapeTeleport(teleportItem)
        }

        val interactions = listOf("Empty", "Rub", "Break", "Teleport", "Operate")
        for (interaction in interactions) {
            if (teleportItem.actions().contains(interaction)) {
                if (teleportItem.interact(interaction)) {
                    Condition.wait({ Players.local().animation() != -1 }, 100, 30)
                    Condition.wait({ Players.local().animation() == -1 && !Players.local().inMotion() }, 100, 50)
                    Condition.sleep(Random.nextInt(600, 1200))
                    return true
                }
            }
        }

        return teleportItem.click()
    }

    fun matchesEmergencyTeleportItem(configuredName: String, candidateItemName: String): Boolean {
        val configured = configuredName.trim()
        val candidate = candidateItemName.trim()
        if (configured.isEmpty() || candidate.isEmpty()) {
            return false
        }
        return candidate.contains(configured, ignoreCase = true)
    }

    private fun isEquipmentTeleportItem(itemName: String): Boolean =
        EQUIPMENT_TELEPORT_ITEMS.any { it.equals(itemName, ignoreCase = true) }

    private fun isMythicalCape(itemName: String): Boolean =
        itemName.equals("mythical cape", ignoreCase = true)

    private fun isArdougneCloak(itemName: String): Boolean =
        itemName.equals(ARDOUGNE_CLOAK_NAME, ignoreCase = true)

    private fun useMythicalCapeTeleport(item: Item): Boolean {
        if (!item.valid()) {
            return false
        }
        if (!Inventory.opened()) {
            Inventory.open()
            Condition.wait({ Inventory.opened() }, 125, 12)
        }
        if (!item.actions().contains("Teleport")) {
            return false
        }
        if (!item.interact("Teleport")) {
            return false
        }
        Condition.wait({ Players.local().animation() != -1 }, 100, 30)
        Condition.wait({ Players.local().animation() == -1 && !Players.local().inMotion() }, 100, 50)
        Condition.sleep(Random.nextInt(600, 1200))
        return true
    }

    private fun useEquipmentTeleport(itemName: String): Boolean {
        if (Equipment.stream().nameContains(itemName).isEmpty()) {
            return false
        }
        if (!openEquipmentTab()) {
            return false
        }

        val teleportComponent = Components.stream(387).action("Teleport").firstOrNull()
        if (teleportComponent == null || !teleportComponent.valid() || !teleportComponent.visible()) {
            return false
        }

        if (!teleportComponent.interact("Teleport")) {
            return false
        }

        Condition.sleep(Random.nextInt(80, 120))
        Condition.wait({ Players.local().animation() != -1 }, 100, 30)
        Condition.wait({ Players.local().animation() == -1 && !Players.local().inMotion() }, 100, 50)
        Condition.sleep(Random.nextInt(600, 1200))

        if (!Inventory.opened()) {
            Inventory.open()
            Condition.wait({ Inventory.opened() }, 150, 8)
        }
        return true
    }

    private fun useArdougneCloakTeleportFromInventory(): Boolean {
        val cloak = Inventory.stream().nameContains(ARDOUGNE_CLOAK_NAME).first()
        if (!cloak.valid() || !cloak.actions().contains(ARDOUGNE_CLOAK_TELEPORT_ACTION)) {
            return false
        }
        if (!cloak.interact(ARDOUGNE_CLOAK_TELEPORT_ACTION)) {
            return false
        }
        Condition.wait({ Players.local().animation() != -1 }, 100, 30)
        Condition.wait({ Players.local().animation() == -1 && !Players.local().inMotion() }, 100, 50)
        Condition.sleep(Random.nextInt(600, 1200))
        return true
    }

    private fun useArdougneCloakTeleportFromEquipment(): Boolean {
        val hasCloakEquipped = Equipment.stream().nameContains(ARDOUGNE_CLOAK_NAME).isNotEmpty()
        if (!hasCloakEquipped || !openEquipmentTab()) {
            return false
        }
        val cloak = Equipment.stream().nameContains(ARDOUGNE_CLOAK_NAME).first()
        if (!cloak.valid() || !cloak.actions().contains(ARDOUGNE_CLOAK_TELEPORT_ACTION)) {
            return false
        }
        if (!cloak.interact(ARDOUGNE_CLOAK_TELEPORT_ACTION)) {
            return false
        }
        Condition.wait({ Players.local().animation() != -1 }, 100, 30)
        Condition.wait({ Players.local().animation() == -1 && !Players.local().inMotion() }, 100, 50)
        Condition.sleep(Random.nextInt(600, 1200))
        if (!Inventory.opened()) {
            Inventory.open()
            Condition.wait({ Inventory.opened() }, 150, 8)
        }
        return true
    }

    private fun openEquipmentTab(): Boolean {
        if (Game.tab() == Game.Tab.EQUIPMENT) {
            return true
        }
        if (!Game.tab(Game.Tab.EQUIPMENT)) {
            return false
        }
        Condition.wait({ Game.tab() == Game.Tab.EQUIPMENT }, 125, 12)
        return Game.tab() == Game.Tab.EQUIPMENT
    }

    private fun isTeleportToHouseSpell(name: String): Boolean =
        name.equals(TELEPORT_TO_HOUSE_NAME, ignoreCase = true)

    private fun castTeleportToHouse(): Boolean {
        val spell = Magic.Spell.TELEPORT_TO_HOUSE
        if (!spell.canCast()) {
            return false
        }
        if (!spell.cast()) {
            return false
        }
        Condition.wait({ Players.local().animation() != -1 }, 100, 30)
        Condition.wait({ Players.local().animation() == -1 && !Players.local().inMotion() }, 100, 50)
        Condition.sleep(Random.nextInt(600, 1200))
        return true
    }
}

