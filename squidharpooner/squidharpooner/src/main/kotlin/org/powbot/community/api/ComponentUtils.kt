package org.powbot.community.api

object ComponentUtils {
    fun cleanName(name: String): String =
        name.replace(Regex("<[^>]*>"), "").trim()
}

