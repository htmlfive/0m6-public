package org.powbot.community.api

object ReequipDecider {
    fun <T> hasAnyReequipableMissing(missing: List<T>, inInventory: (T) -> Boolean): Boolean {
        return missing.any { inInventory(it) }
    }

    fun <T> canReequipAllMissing(missing: List<T>, inInventory: (T) -> Boolean): Boolean {
        if (missing.isEmpty()) {
            return true
        }
        return missing.all { inInventory(it) }
    }
}

