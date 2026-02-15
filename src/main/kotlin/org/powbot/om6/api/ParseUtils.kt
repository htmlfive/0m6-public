package org.powbot.om6.api

/**
 * Common parsing utility functions for item configurations and text.
 */
object ParseUtils {

    /**
     * Simple data class for item configuration.
     */
    data class ItemConfig(
        val name: String,
        val quantity: Int = 1
    )

    /**
     * Parses a comma-separated string into a list of trimmed, non-empty strings.
     *
     * @param input The comma-separated string to parse
     * @return A list of trimmed strings with empty entries filtered out
     */
    fun parseCommaSeparatedList(input: String): List<String> {
        return input.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * Parses a comma or semicolon-separated item string into ItemConfig objects.
     * Format: "Item Name, Item Name:quantity, Item Name:quantity" or "Item Name; Item Name:quantity; Item Name:quantity"
     * Example: "Rune arrow:10, Adamant arrow, Fire rune:1000" or "Rune arrow:10; Adamant arrow; Fire rune:1000"
     *
     * @param itemString The comma or semicolon-separated items with optional quantities.
     * @param defaultQuantity Default quantity if not specified (default: 1)
     * @return A list of ItemConfig objects.
     */
    fun parseItemNames(itemString: String, defaultQuantity: Int = 1): List<ItemConfig> {
        if (itemString.isBlank()) return emptyList()

        return itemString.split(Regex("[,;]"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { entry ->
                val parts = entry.split(":")
                val name = parts[0].trim()

                if (name.isEmpty()) return@mapNotNull null

                val quantity = if (parts.size > 1) {
                    parts[1].trim().toIntOrNull() ?: defaultQuantity
                } else {
                    defaultQuantity
                }

                ItemConfig(name = name, quantity = quantity)
            }
    }

    /**
     * Parses item names with support for "all" quantity (-1).
     * Format: "Item Name:all" or "Item Name:10"
     *
     * @param itemString The item string to parse
     * @return A list of ItemConfig objects where -1 means "all"
     */
    fun parseItemNamesWithAll(itemString: String): List<ItemConfig> {
        if (itemString.isBlank()) return emptyList()

        return itemString.split(Regex("[,;]"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { entry ->
                val parts = entry.split(":")
                val name = parts[0].trim()

                if (name.isEmpty()) return@mapNotNull null

                val quantity = if (parts.size > 1) {
                    val quantityStr = parts[1].trim()
                    if (quantityStr.equals("all", ignoreCase = true)) {
                        -1
                    } else {
                        quantityStr.toIntOrNull() ?: -1
                    }
                } else {
                    -1 // Default to all
                }

                ItemConfig(name = name, quantity = quantity)
            }
    }
}
