package org.powbot.om6.api

object ComponentUtils {
    fun cleanName(name: String): String =
        name.replace(Regex("<[^>]*>"), "").trim()
}
