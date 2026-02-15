package org.powbot.om6.api

/**
 * Common formatting utility functions.
 */
object FormatUtils {

    /**
     * Formats a number with K/M suffixes.
     * @param number The number to format
     * @return Formatted string (e.g., "1.5m", "250k")
     */
    fun formatNumber(number: Long): String {
        return when {
            number >= 1_000_000 -> String.format("%.1fm", number / 1_000_000.0)
            number >= 1_000 -> String.format("%.1fk", number / 1_000.0)
            else -> number.toString()
        }
    }

    /**
     * Formats a number with K/M suffixes (Int version).
     * @param number The number to format
     * @return Formatted string (e.g., "1.5m", "250k")
     */
    fun formatNumber(number: Int): String {
        return formatNumber(number.toLong())
    }

    /**
     * Formats distance to a friendly string.
     * @param distance The distance value
     * @return Formatted distance string (e.g., "15.2 tiles")
     */
    fun formatDistance(distance: Double): String {
        return "%.1f tiles".format(distance)
    }

    /**
     * Formats time in milliseconds to a readable string.
     * @param millis Time in milliseconds
     * @return Formatted time string (e.g., "1h 23m 45s")
     */
    fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60)) % 24

        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    /**
     * Formats a percentage with one decimal place.
     * @param value The value
     * @param total The total
     * @return Formatted percentage string (e.g., "45.2%")
     */
    fun formatPercentage(value: Int, total: Int): String {
        if (total == 0) return "0.0%"
        return "%.1f%%".format((value.toDouble() / total.toDouble()) * 100)
    }

    /**
     * Formats a percentage with one decimal place.
     * @param value The value
     * @param total The total
     * @return Formatted percentage string (e.g., "45.2%")
     */
    fun formatPercentage(value: Long, total: Long): String {
        if (total == 0L) return "0.0%"
        return "%.1f%%".format((value.toDouble() / total.toDouble()) * 100)
    }
}
