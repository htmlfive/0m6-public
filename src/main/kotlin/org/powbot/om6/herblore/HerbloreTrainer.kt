package org.powbot.om6.herblore

import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.AbstractScript
import org.powbot.api.script.OptionType
import org.powbot.api.script.ScriptCategory
import org.powbot.api.script.ScriptConfiguration
import org.powbot.api.script.ScriptManifest
import org.powbot.api.script.ValueChanged
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.om6.api.ScriptLogging
import org.powbot.om6.herblore.data.HerbloreRuntimeConfig
import org.powbot.om6.herblore.data.HerbloreUi
import org.powbot.om6.herblore.services.HerbloreBankingService
import org.powbot.om6.herblore.services.HerbloreConfigService
import org.powbot.om6.herblore.services.InventoryInteractionService
import org.powbot.om6.herblore.services.ProductionWidgetService
import org.powbot.om6.herblore.tasks.CleanHerbsTask
import org.powbot.om6.herblore.tasks.HerbloreTask
import org.powbot.om6.herblore.tasks.MakeFinishedPotionsTask
import org.powbot.om6.herblore.tasks.MakeUnfinishedPotionsTask

@ScriptManifest(
    name = "0m6 Herblore Trainer",
    description = "Bankstanding herblore script with herb cleaning, unfinished potions, and finished potions.",
    version = "1.0.1",
    author = "0m6",
    category = ScriptCategory.Herblore
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            name = "Instructions",
            description = "Choose mode in General page. Cleaning/Unfinished can run make-all by level. Finished potions are one-at-a-time.",
            optionType = OptionType.INFO
        ),
        ScriptConfiguration(
            name = HerbloreUi.CONFIG_PAGE_OPTION,
            description = "Select configuration category.",
            optionType = OptionType.STRING,
            allowedValues = [
                HerbloreUi.PAGE_GENERAL,
                HerbloreUi.PAGE_HERB_CLEANING,
                HerbloreUi.PAGE_UNFINISHED,
                HerbloreUi.PAGE_FINISHED,
                HerbloreUi.PAGE_FINISHED_QUEUE,
                HerbloreUi.PAGE_WHOLE_SHOW
            ],
            defaultValue = HerbloreUi.PAGE_GENERAL,
            visible = true
        ),
        ScriptConfiguration(
            name = HerbloreUi.OPTION_MODE,
            description = "Which workflow to execute.",
            optionType = OptionType.STRING,
            allowedValues = [
                "Herb cleaning",
                "Unfinished potions",
                "Finished potions"
            ],
            defaultValue = "Herb cleaning",
            visible = false
        ),
        ScriptConfiguration(
            name = HerbloreUi.OPTION_AUTO_CLEAN_AND_UNF,
            description = "If enabled, automatically clean all eligible grimy herbs and then make all eligible unfinished potions.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "false",
            visible = true
        ),
        ScriptConfiguration(
            name = HerbloreUi.OPTION_CLEANING_HERB,
            description = "Herb to clean when make-all is disabled.",
            optionType = OptionType.STRING,
            allowedValues = [
                "Guam",
                "Marrentill",
                "Tarromin",
                "Harralander",
                "Ranarr",
                "Toadflax",
                "Irit",
                "Avantoe",
                "Kwuarm",
                "Snapdragon",
                "Cadantine",
                "Lantadyme",
                "Dwarf weed",
                "Torstol"
            ],
            defaultValue = "Ranarr",
            visible = false
        ),
        ScriptConfiguration(
            name = HerbloreUi.OPTION_CLEANING_MAKE_ALL,
            description = "Clean every herb your level can clean, based on bank availability.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "false",
            visible = false
        ),
        ScriptConfiguration(
            name = HerbloreUi.OPTION_UNFINISHED_HERB,
            description = "Unfinished potion herb when make-all is disabled.",
            optionType = OptionType.STRING,
            allowedValues = [
                "Guam",
                "Marrentill",
                "Tarromin",
                "Harralander",
                "Ranarr",
                "Toadflax",
                "Irit",
                "Avantoe",
                "Kwuarm",
                "Snapdragon",
                "Cadantine",
                "Lantadyme",
                "Dwarf weed",
                "Torstol"
            ],
            defaultValue = "Ranarr",
            visible = false
        ),
        ScriptConfiguration(
            name = HerbloreUi.OPTION_UNFINISHED_MAKE_ALL,
            description = "Make all unfinished potions your level can make, based on bank availability.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "false",
            visible = false
        ),
        ScriptConfiguration(
            name = HerbloreUi.OPTION_FINISHED_POTION,
            description = "Finished potion to make (one recipe at a time).",
            optionType = OptionType.STRING,
            allowedValues = [
                "Attack potion",
                "Antipoison",
                "Strength potion",
                "Serum 207",
                "Compost potion",
                "Restore potion",
                "Energy potion",
                "Defence potion",
                "Agility potion",
                "Combat potion",
                "Prayer potion",
                "Super attack",
                "Superantipoison",
                "Fishing potion",
                "Super energy",
                "Hunter potion",
                "Super strength",
                "Weapon poison",
                "Super restore",
                "Super defence",
                "Antifire potion",
                "Ranging potion",
                "Magic potion",
                "Zamorak brew",
                "Saradomin brew"
            ],
            defaultValue = "Super energy",
            visible = false
        ),
        ScriptConfiguration(name = "Queue Attack potion", description = "Enable Attack potion in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "true", visible = false),
        ScriptConfiguration(name = "Queue Antipoison", description = "Enable Antipoison in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "false", visible = false),
        ScriptConfiguration(name = "Queue Strength potion", description = "Enable Strength potion in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "false", visible = false),
        ScriptConfiguration(name = "Queue Serum 207", description = "Enable Serum 207 in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "false", visible = false),
        ScriptConfiguration(name = "Queue Compost potion", description = "Enable Compost potion in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "false", visible = false),
        ScriptConfiguration(name = "Queue Restore potion", description = "Enable Restore potion in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "false", visible = false),
        ScriptConfiguration(name = "Queue Energy potion", description = "Enable Energy potion in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "true", visible = false),
        ScriptConfiguration(name = "Queue Defence potion", description = "Enable Defence potion in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "false", visible = false),
        ScriptConfiguration(name = "Queue Agility potion", description = "Enable Agility potion in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "false", visible = false),
        ScriptConfiguration(name = "Queue Combat potion", description = "Enable Combat potion in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "false", visible = false),
        ScriptConfiguration(name = "Queue Prayer potion", description = "Enable Prayer potion in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "true", visible = false),
        ScriptConfiguration(name = "Queue Super attack", description = "Enable Super attack in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "true", visible = false),
        ScriptConfiguration(name = "Queue Superantipoison", description = "Enable Superantipoison in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "false", visible = false),
        ScriptConfiguration(name = "Queue Fishing potion", description = "Enable Fishing potion in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "false", visible = false),
        ScriptConfiguration(name = "Queue Super energy", description = "Enable Super energy in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "true", visible = false),
        ScriptConfiguration(name = "Queue Hunter potion", description = "Enable Hunter potion in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "false", visible = false),
        ScriptConfiguration(name = "Queue Super strength", description = "Enable Super strength in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "true", visible = false),
        ScriptConfiguration(name = "Queue Weapon poison", description = "Enable Weapon poison in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "false", visible = false),
        ScriptConfiguration(name = "Queue Super restore", description = "Enable Super restore in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "true", visible = false),
        ScriptConfiguration(name = "Queue Super defence", description = "Enable Super defence in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "true", visible = false),
        ScriptConfiguration(name = "Queue Antifire potion", description = "Enable Antifire potion in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "true", visible = false),
        ScriptConfiguration(name = "Queue Ranging potion", description = "Enable Ranging potion in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "true", visible = false),
        ScriptConfiguration(name = "Queue Magic potion", description = "Enable Magic potion in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "false", visible = false),
        ScriptConfiguration(name = "Queue Zamorak brew", description = "Enable Zamorak brew in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "false", visible = false),
        ScriptConfiguration(name = "Queue Saradomin brew", description = "Enable Saradomin brew in finished queue.", optionType = OptionType.BOOLEAN, defaultValue = "false", visible = false)
    ]
)
class HerbloreTrainer : AbstractScript() {
    enum class WholeShowPhase {
        NONE,
        CLEANING,
        UNFINISHED,
        FINISHED,
        DONE
    }

    val bankingService = HerbloreBankingService(
        onAction = { logAction("Banking: $it") },
        onWarn = { logWarn("Banking: $it") }
    )
    val inventoryInteractionService = InventoryInteractionService()
    val widgetService = ProductionWidgetService()

    private val configService = HerbloreConfigService(this)
    private lateinit var tasks: List<HerbloreTask>

    private var runtimeConfig: HerbloreRuntimeConfig = configService.read()
    private var taskStatus: String = "Idle"
    private var stopRequested: Boolean = false
    private var wholeShowPhase: WholeShowPhase = WholeShowPhase.NONE

    override fun onStart() {
        runtimeConfig = configService.read()
        updateConfigPageVisibility(currentConfigPage())
        syncWholeShowPhase()
        logAction("Herblore Trainer starting - mode=${runtimeConfig.mode.uiValue}")

        tasks = listOf(
            CleanHerbsTask(this),
            MakeUnfinishedPotionsTask(this),
            MakeFinishedPotionsTask(this)
        )

        addPaint(
            PaintBuilder.newBuilder()
                .trackSkill(Skill.Herblore)
                .addString("Mode") { paintModeLabel() }
                .addString("Task") { taskStatus }
                .addString("Whole Show") { if (runtimeConfig.wholeShowEnabled) "ACTIVE" else "OFF" }
                .addString("Clean All Herbs") { wholeShowCleaningStatus() }
                .addString("Make All Unf Pots") { wholeShowUnfinishedStatus() }
                .addString("Make All Finished Pots") { wholeShowFinishedStatus() }
                .build()
        )
    }

    override fun poll() {
        runtimeConfig = configService.read()
        syncWholeShowPhase()
        val task = tasks.firstOrNull { it.shouldExecute() }
        if (task != null) {
            task.execute()
            return
        }
        if (runtimeConfig.autoCleanAndUnfAll) {
            stopWithNotification("Auto clean + unfinished complete (no eligible supplies). Stopping script.")
            return
        }
        if (runtimeConfig.wholeShowEnabled) {
            if (wholeShowPhase == WholeShowPhase.DONE) {
                stopWithNotification("The whole show complete. Stopping script.")
            }
        }
    }

    fun runtimeConfig(): HerbloreRuntimeConfig = runtimeConfig

    fun setTaskStatus(status: String) {
        taskStatus = status
        logInfo("STATUS: $status")
    }

    fun stopWithNotification(reason: String) {
        if (stopRequested) return
        stopRequested = true
        setTaskStatus(reason)
        ScriptLogging.stopWithNotification(this, reason)
    }

    fun logInfo(message: String) {
        ScriptLogging.info(logger, message)
    }

    fun logWarn(message: String) {
        ScriptLogging.warn(logger, message)
    }

    fun logAction(message: String) {
        ScriptLogging.action(logger, message)
    }

    fun wholeShowPhase(): WholeShowPhase = wholeShowPhase

    fun advanceWholeShowToUnfinished() {
        if (wholeShowPhase == WholeShowPhase.CLEANING) {
            wholeShowPhase = WholeShowPhase.UNFINISHED
            setTaskStatus("Whole show: moving to unfinished")
        }
    }

    fun advanceWholeShowToFinished() {
        if (wholeShowPhase == WholeShowPhase.UNFINISHED) {
            wholeShowPhase = WholeShowPhase.FINISHED
            setTaskStatus("Whole show: moving to finished")
        }
    }

    fun completeWholeShow() {
        if (wholeShowPhase == WholeShowPhase.FINISHED) {
            wholeShowPhase = WholeShowPhase.DONE
            setTaskStatus("Whole show complete")
        }
    }

    @ValueChanged(HerbloreUi.CONFIG_PAGE_OPTION)
    @Suppress("unused")
    fun onConfigurationPageChanged(page: String) {
        updateConfigPageVisibility(page)
    }

    private fun updateConfigPageVisibility(page: String) {
        val visibleOptions = HerbloreUi.PAGE_OPTION_GROUPS[page]?.toSet() ?: emptySet()
        HerbloreUi.ALL_PAGE_OPTIONS.forEach { optionName ->
            updateVisibility(optionName, visibleOptions.contains(optionName))
        }
    }

    private fun currentConfigPage(): String {
        return runCatching { getOption<String>(HerbloreUi.CONFIG_PAGE_OPTION) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: HerbloreUi.PAGE_GENERAL
    }

    private fun syncWholeShowPhase() {
        if (runtimeConfig.wholeShowEnabled) {
            if (wholeShowPhase == WholeShowPhase.NONE) {
                wholeShowPhase = WholeShowPhase.CLEANING
            }
            return
        }
        wholeShowPhase = WholeShowPhase.NONE
    }

    private fun wholeShowCleaningStatus(): String {
        if (!runtimeConfig.wholeShowEnabled) return "N/A"
        return when (wholeShowPhase) {
            WholeShowPhase.NONE -> "Pending"
            WholeShowPhase.CLEANING -> "In progress"
            WholeShowPhase.UNFINISHED, WholeShowPhase.FINISHED, WholeShowPhase.DONE -> "Done"
        }
    }

    private fun wholeShowUnfinishedStatus(): String {
        if (!runtimeConfig.wholeShowEnabled) return "N/A"
        return when (wholeShowPhase) {
            WholeShowPhase.NONE, WholeShowPhase.CLEANING -> "Pending"
            WholeShowPhase.UNFINISHED -> "In progress"
            WholeShowPhase.FINISHED, WholeShowPhase.DONE -> "Done"
        }
    }

    private fun wholeShowFinishedStatus(): String {
        if (!runtimeConfig.wholeShowEnabled) return "N/A"
        return when (wholeShowPhase) {
            WholeShowPhase.NONE, WholeShowPhase.CLEANING, WholeShowPhase.UNFINISHED -> "Pending"
            WholeShowPhase.FINISHED -> "In progress"
            WholeShowPhase.DONE -> "Done"
        }
    }

    private fun paintModeLabel(): String {
        return when (currentConfigPage()) {
            HerbloreUi.PAGE_HERB_CLEANING -> "Herb cleaning"
            HerbloreUi.PAGE_UNFINISHED -> "Unfinished potions"
            HerbloreUi.PAGE_FINISHED -> "Finished potions"
            HerbloreUi.PAGE_FINISHED_QUEUE -> "Finished queue"
            HerbloreUi.PAGE_WHOLE_SHOW -> "The whole show"
            else -> runtimeConfig.mode.uiValue
        }
    }
}


fun main() {
    val script = HerbloreTrainer()
    script.startScript("localhost", "0m6", false)
}
