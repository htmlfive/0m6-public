package org.powbot.community.api

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Item
import org.powbot.api.rt4.Prayer
import org.powbot.api.rt4.Skills
import org.powbot.api.rt4.walking.model.Skill

object PrayerUtils {
    fun getPrayerLevel(): Int = Skills.level(Skill.Prayer)

    private fun getPrayerEffect(prayerName: String): Prayer.Effect? = when (prayerName) {
        "Protect from Melee" -> Prayer.Effect.PROTECT_FROM_MELEE
        "Protect from Magic" -> Prayer.Effect.PROTECT_FROM_MAGIC
        "Protect from Missiles" -> Prayer.Effect.PROTECT_FROM_MISSILES
        "Burst of Strength" -> Prayer.Effect.BURST_OF_STRENGTH
        "Superhuman Strength" -> Prayer.Effect.SUPERHUMAN_STRENGTH
        "Ultimate Strength" -> Prayer.Effect.ULTIMATE_STRENGTH
        "Chivalry" -> Prayer.Effect.CHIVALRY
        "Piety" -> Prayer.Effect.PIETY
        "Sharp Eye" -> Prayer.Effect.SHARP_EYE
        "Hawk Eye" -> Prayer.Effect.HAWK_EYE
        "Eagle Eye" -> Prayer.Effect.EAGLE_EYE
        "Rigour" -> Prayer.Effect.RIGOUR
        "Mystic Will" -> Prayer.Effect.MYSTIC_WILL
        "Mystic Lore" -> Prayer.Effect.MYSTIC_LORE
        "Mystic Might" -> Prayer.Effect.MYSTIC_MIGHT
        "Augury" -> Prayer.Effect.AUGURY
        else -> null
    }

    fun isPrayerActive(prayerName: String): Boolean {
        val effect = getPrayerEffect(prayerName) ?: return false
        return Prayer.prayerActive(effect)
    }

    fun activatePrayer(prayerName: String): Boolean {
        if (isPrayerActive(prayerName)) return true
        val effect = getPrayerEffect(prayerName) ?: return false
        return if (Prayer.prayer(effect, true)) {
            Condition.wait({ isPrayerActive(prayerName) }, 100, 10)
        } else {
            false
        }
    }

    fun deactivatePrayer(prayerName: String): Boolean {
        if (!isPrayerActive(prayerName)) return true
        val effect = getPrayerEffect(prayerName) ?: return false
        return if (Prayer.prayer(effect, false)) {
            Condition.wait({ !isPrayerActive(prayerName) }, 100, 10)
        } else {
            false
        }
    }

    fun getPrayerPotion(): Item {
val prayerPotions = arrayOf(
            "Prayer potion(1)", "Prayer potion(2)", "Prayer potion(3)", "Prayer potion(4)",
            "Super restore(1)", "Super restore(2)", "Super restore(3)", "Super restore(4)",
            "Sanfew serum(1)", "Sanfew serum(2)", "Sanfew serum(3)", "Sanfew serum(4)",
            "Moonlight moth mix (1)", "Moonlight moth mix (2)"
        )
        return Inventory.stream().name(*prayerPotions).first()
    }

    fun drinkPrayerPotion(potion: Item): Boolean {
        if (!potion.valid()) return false
        Inventory.open()
        Condition.sleep(Random.nextInt(90, 120))
        return potion.click()
    }

    fun hasMoonlightMoth(): Boolean =
        Inventory.stream().name("Moonlight moth").isNotEmpty()

    fun releaseMoonlightMoth(): Boolean {
        val moth = Inventory.stream().name("Moonlight moth").first()
        if (!moth.valid()) return false
        Inventory.open()
        Condition.sleep(Random.nextInt(90, 120))
        return if (moth.interact("Release")) {
            Condition.wait({ getPrayerLevel() > 0 }, 100, 20)
        } else {
            false
        }
    }

    fun isOutOfPrayerRestoreItems(): Boolean {
        val hasPrayerPotion = Inventory.stream()
            .nameContains("Prayer potion", "Super restore", "Sanfew serum", "Moonlight moth mix")
            .isNotEmpty()
        return !hasPrayerPotion && !hasMoonlightMoth()
    }

    fun hasProtectionPrayerEnabled(protectionPrayer: String): Boolean =
        protectionPrayer != "None"
}

