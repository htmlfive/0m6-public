package org.powbot.community.api

import org.powbot.api.Notifications
import org.powbot.api.script.AbstractScript
import org.powbot.mobile.script.ScriptManager
import org.slf4j.Logger
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ScriptLogging {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    fun timestampMs(): String {
        return LocalDateTime.now().format(formatter)
    }

    fun info(logger: Logger, message: String) {
        logger.info("${timestampMs()} $message")
    }

    fun action(logger: Logger, message: String) {
        logger.info("${timestampMs()} ACTION: $message")
    }

    fun warn(logger: Logger, message: String) {
        logger.info("${timestampMs()} WARN: $message")
    }

    fun error(logger: Logger, message: String) {
        logger.info("${timestampMs()} ERROR: $message")
    }

    fun stopWithNotification(script: AbstractScript, reason: String) {
        warn(script.logger, "Stopping script: $reason")
        Notifications.showNotification(reason)
        ScriptManager.stop()
    }
}

