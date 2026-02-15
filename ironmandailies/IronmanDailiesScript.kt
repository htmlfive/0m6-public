package org.powbot.om6.ironmandailies

import org.powbot.api.Condition
import org.powbot.api.script.AbstractScript
import org.powbot.api.script.OptionType
import org.powbot.api.script.ScriptCategory
import org.powbot.api.script.ScriptConfiguration
import org.powbot.api.script.ScriptManifest
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.om6.api.ScriptLogging

sealed class DailyTaskResult {
    data object InProgress : DailyTaskResult()
    data object Completed : DailyTaskResult()
    data class Failed(val reason: String) : DailyTaskResult()
}

interface DailyTask {
    val name: String
    val subStage: String
    fun poll(): DailyTaskResult
}

@ScriptManifest(
    name = "0m6 Ironman Dailies",
    description = "Runs configured Ironman daily tasks in sequence.",
    version = "1.1.2",
    author = "0m6",
    category = ScriptCategory.Other
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            name = "Kingdom Start Step Help",
            description = "Debug start step dropdown for Kingdom substages.",
            optionType = OptionType.INFO
        ),
        ScriptConfiguration(
            name = "Kingdom Start Step",
            description = "Debug only. Choose where Kingdom starts.",
            optionType = OptionType.STRING,
            defaultValue = "1 - PREPARE_SUPPLIES",
            allowedValues = [
                "1 - PREPARE_SUPPLIES",
                "2 - WALK_TO_PATCH",
                "3 - RAKE_FLAX",
                "4 - RAKE_HERBS",
                "5 - DROP_WEEDS",
                "6 - WAIT_FOR_CONTINUE_OR_HOP",
                "7 - HOP_BEFORE_CONTINUE",
                "8 - WALK_TO_GHRIM",
                "9 - COLLECT_GHRIM_FIRST",
                "10 - SELECT_COLLECT_CHAT_OPTION",
                "11 - CLOSE_BEFORE_SECOND_GHRIM",
                "12 - COLLECT_GHRIM_SECOND",
                "13 - CLICK_CHAT_CONTINUE",
                "14 - CLICK_DEPOSIT_WIDGET",
                "15 - FINAL_CHAT_CONTINUE",
                "16 - SEND_DEPOSIT_INPUT",
                "17 - CONTINUE_BEFORE_CLOSE",
                "18 - SECOND_CONTINUE_BEFORE_CLOSE",
                "19 - CLICK_CLOSE_WIDGET",
                "20 - COMPLETE"
            ]
        ),
        ScriptConfiguration(
            name = "Hop Before Continue",
            description = "For Kingdom daily, hop worlds before advancing when continue appears.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "false"
        )
    ]
)
class IronmanDailiesScript : AbstractScript() {

    private val kingdomStartStepRaw: String
        get() = getOption("Kingdom Start Step")

    private val hopBeforeContinue: Boolean
        get() = getOption("Hop Before Continue")

    private val dailyTasks = mutableListOf<DailyTask>()
    private var currentTaskIndex = 0

    private val currentTask: DailyTask?
        get() = dailyTasks.getOrNull(currentTaskIndex)

    override fun onStart() {
        ScriptLogging.info(
            logger,
            "IronmanDailiesScript started. Hop Before Continue: $hopBeforeContinue, Kingdom Start Step: '${kingdomStartStepRaw.trim()}'"
        )
        buildTaskList()
        addPaint(
            PaintBuilder.newBuilder()
                .x(40).y(80)
                .addString("Script") { "Ironman Dailies" }
                .addString("Task Index") { "${currentTaskIndex + 1}/${dailyTasks.size}" }
                .addString("Current Daily") { currentTask?.name ?: "None" }
                .addString("Substage") { currentTask?.subStage ?: "-" }
                .addString("Kingdom Start Step") { kingdomStartStepRaw.trim().ifBlank { "default" } }
                .build()
        )
    }

    override fun poll() {
        val task = currentTask
        if (task == null) {
            ScriptLogging.stopWithNotification(this, "All configured dailies complete.")
            return
        }

        when (val result = task.poll()) {
            is DailyTaskResult.InProgress -> Unit
            is DailyTaskResult.Completed -> {
                ScriptLogging.info(logger, "Daily completed: ${task.name}")
                currentTaskIndex++
                if (currentTask == null) {
                    ScriptLogging.stopWithNotification(this, "All configured dailies complete.")
                    return
                }
            }

            is DailyTaskResult.Failed -> {
                ScriptLogging.stopWithNotification(this, "Daily failed (${task.name}): ${result.reason}")
                return
            }
        }

        Condition.sleep(100)
    }

    private fun buildTaskList() {
        dailyTasks.clear()
        dailyTasks += KingdomDailyTask(
            logger = logger,
            hopBeforeContinue = hopBeforeContinue,
            debugStartStep = kingdomStartStepRaw
        )
    }
}


fun main() {
    val script = IronmanDailiesScript()
    script.startScript("localhost", "0m6", false)
}
