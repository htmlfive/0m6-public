package org.powbot.community.maplefletcher.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Component
import org.powbot.api.rt4.Components
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Players
import org.powbot.community.maplefletcher.MapleFletcher
import org.powbot.community.maplefletcher.MapleFletcherConstants
import java.util.logging.Logger

class FletchMaples(private val script: MapleFletcher) : Task {
    private val logger = Logger.getLogger(FletchMaples::class.java.name)
    private var lastObservedLogCount = -1
    private var lastLogChangeAt = 0L
    private var lastMakeAllClickAt = 0L
    private var idleSinceDuringFletch: Long = -1L

    override fun execute(): Int {
        val logsCount = logCount()
        updateCounters(logsCount)

        if (logsCount <= 0) {
            logger.fine("No maple logs to fletch.")
            return 250
        }
        if (isFletchingInProgress()) return 450

        if (!ensureFletchingInterface()) {
            logger.warning("Unable to open fletching interface.")
            return 300
        }
        val option = findConfiguredOption()
        if (option == null) {
            logger.warning("Unable to locate configured fletching option.")
            return 300
        }
        if (option.click()) {
            lastMakeAllClickAt = System.currentTimeMillis()
            val completed = waitForLogsToFinishOrInterrupt()
            if (!completed) {
                retryInterruptedFletch()
            }
            Condition.wait({ Inventory.stream().name(MapleFletcherConstants.MAPLE_LOG_NAME).isEmpty() }, 500, 120)
        }
        return 250
    }

    private fun ensureFletchingInterface(): Boolean {
        if (findConfiguredOption() != null) return true
        if (isFletchingInProgress()) return true

        val knife = Inventory.stream().name(MapleFletcherConstants.KNIFE_NAME).first()
        val log = Inventory.stream().name(MapleFletcherConstants.MAPLE_LOG_NAME).first()
        if (!knife.valid() || !log.valid()) return false
        if (!knife.useOn(log)) return false
        Condition.wait({ findConfiguredOption() != null }, 250, 12)
        return findConfiguredOption() != null
    }

    private fun findConfiguredOption(): Component? {
        val desired = normalizeUiText(script.configuredFletchTo.optionMatch)
        val option = Components.stream(270)
            .filtered {
                if (!it.valid() || !it.visible()) return@filtered false
                if (it.actions().none { action -> action.contains("Make", ignoreCase = true) }) return@filtered false

                val text = normalizeUiText(it.text())
                val name = normalizeUiText(it.name())
                text.contains(desired) || name.contains(desired)
            }
            .firstOrNull()
        return if (option != null && option.valid()) option else null
    }

    private fun retryInterruptedFletch() {
        val knife = Inventory.stream().name(MapleFletcherConstants.KNIFE_NAME).first()
        val log = Inventory.stream().name(MapleFletcherConstants.MAPLE_LOG_NAME).first()
        if (!knife.valid() || !log.valid()) return
        if (!knife.useOn(log)) return
        if (!Condition.wait({ findConfiguredOption() != null }, 150, 10)) return

        val option = findConfiguredOption() ?: return
        if (option.click()) {
            lastMakeAllClickAt = System.currentTimeMillis()
        }
    }

    private fun waitForLogsToFinishOrInterrupt(): Boolean {
        idleSinceDuringFletch = -1L
        val reachedTerminalState = Condition.wait(
            {
                val noLogsLeft = Inventory.stream().name(MapleFletcherConstants.MAPLE_LOG_NAME).isEmpty()
                if (noLogsLeft) return@wait true

                val idle = Players.local().animation() == -1
                if (idle) {
                    if (idleSinceDuringFletch < 0L) {
                        idleSinceDuringFletch = System.currentTimeMillis()
                    }
                    val idleForMs = System.currentTimeMillis() - idleSinceDuringFletch
                    idleForMs >= 2000L
                } else {
                    idleSinceDuringFletch = -1L
                    false
                }
            },
            100,
            600
        )

        if (!reachedTerminalState) return false
        return Inventory.stream().name(MapleFletcherConstants.MAPLE_LOG_NAME).isEmpty()
    }

    private fun normalizeUiText(raw: String): String {
        return raw
            .replace(Regex("<[^>]*>"), " ")
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun updateCounters(logsCount: Int) {
        if (lastObservedLogCount == -1) {
            lastObservedLogCount = logsCount
            lastLogChangeAt = System.currentTimeMillis()
        } else if (logsCount != lastObservedLogCount) {
            lastObservedLogCount = logsCount
            lastLogChangeAt = System.currentTimeMillis()
        }
    }

    private fun isFletchingInProgress(): Boolean {
        val player = Players.local()
        if (player.animation() != -1) return true

        val now = System.currentTimeMillis()
        if (now - lastMakeAllClickAt < 3500L) return true
        return now - lastLogChangeAt < 2500L
    }

    private fun logCount(): Int = Inventory.stream().name(MapleFletcherConstants.MAPLE_LOG_NAME).count(true).toInt()

    override fun toString(): String = "Fletching maple logs"
}
