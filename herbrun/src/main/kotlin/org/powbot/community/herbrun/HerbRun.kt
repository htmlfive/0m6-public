package org.powbot.community.herbrun

import org.powbot.api.Condition
import org.powbot.api.Notifications
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.AbstractScript
import org.powbot.api.script.OptionType
import org.powbot.api.script.ScriptConfiguration
import org.powbot.api.script.ScriptManifest
import org.powbot.api.script.ValueChanged
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.api.script.paint.PaintItem
import org.powbot.mobile.script.ScriptManager
import org.powbot.community.api.BlockingWorldHopService
import org.powbot.community.api.MovementUtils
import org.powbot.community.api.WorldHopIntervalScheduler
import org.powbot.community.herbrun.config.*
import org.powbot.community.herbrun.services.BigCompostBinService
import org.powbot.community.herbrun.services.HerbRunBankingService
import org.powbot.community.herbrun.services.HerbRunConfigFactory
import org.powbot.community.herbrun.services.HerbRunUiService
import org.powbot.community.herbrun.services.LimpwurtService
import org.powbot.community.herbrun.services.PatchStateResolver
import org.powbot.community.herbrun.services.PatchProcessingService
import org.powbot.community.herbrun.services.PickpocketService
import org.powbot.community.herbrun.services.PreflightBankingService
import org.powbot.community.herbrun.services.RunCycleService
import org.powbot.community.herbrun.services.ToolLeprechaunService
import org.powbot.community.herbrun.tasks.*
import com.google.common.eventbus.Subscribe
import org.powbot.api.event.PaintCheckboxChangedEvent
import kotlin.collections.ArrayDeque
import kotlin.math.abs
import kotlin.random.Random
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val OPTION_HERB_TYPE = "Herb Type"
private const val OPTION_FALLBACK_SEED_1 = "Fallback Seed 1"
private const val OPTION_FALLBACK_SEED_2 = "Fallback Seed 2"
private const val OPTION_FALLBACK_SEED_3 = "Fallback Seed 3"
private const val OPTION_FALLBACK_SEED_4 = "Fallback Seed 4"
private const val OPTION_FALLBACK_SEED_5 = "Fallback Seed 5"
private const val OPTION_COMPOST_TYPE = "Compost Type"
private const val OPTION_ADDITIONAL_WITHDRAWALS = "Additional Withdrawals"
private const val OPTION_REQUIRES_RAKE = "Requires rake"
private const val OPTION_REQUIRES_SEED_DIBBER = "Requires seed dibber"
private const val OPTION_LOOP_RUNS = "Loop Runs"
private const val OPTION_NOTE_HERBS = "Note Herbs"
private const val OPTION_PICKPOCKET_BETWEEN_RUNS = "Pickpocket Between Runs"
private const val OPTION_PICKPOCKET_ENABLE_LIMPWURT = "Enable Limpwurt farming"
private const val OPTION_START_WITH_PICKPOCKET = "Start Pickpocketing"
private const val OPTION_PICKPOCKET_WINE_WITHDRAW = "Pickpocket Wine Withdraw Amount"
private const val OPTION_PICKPOCKET_HEAL_DEFICIT = "Pickpocket Heal Deficit"
private const val OPTION_PICKPOCKET_DROP_LIST = "Pickpocket Drop List"
private const val OPTION_MASTER_FARMER_TILE = "Master Farmer Tile"
private const val OPTION_ENABLE_COOLDOWN_WORLD_HOP = "Enable Cooldown World Hop"
private const val OPTION_HOP_AFTER_EACH_RUN = "Hop After Each Run"
private const val OPTION_COOLDOWN_WORLD_HOP_MIN_MINUTES = "Cooldown Hop Min Minutes"
private const val OPTION_COOLDOWN_WORLD_HOP_MAX_MINUTES = "Cooldown Hop Max Minutes"
private const val OPTION_RUN_BIG_COMPOST_BIN = "Run Big Compost Bin"
private const val START_RUN_NOW_CHECKBOX_ID = "herbrun_start_run_now"
private const val SHOW_TRACKED_ITEMS_CHECKBOX_ID = "herbrun_show_tracked_items"
private const val SHOW_RUN_CONFIG_CHECKBOX_ID = "herbrun_show_run_config"
private const val COOLDOWN_HOP_MIN_MINUTES = 30
private const val COOLDOWN_HOP_MAX_MINUTES = 60
private const val COOLDOWN_WITHOUT_COMPOST_BIN_MINUTES = 80L
private const val COOLDOWN_WITH_COMPOST_BIN_MINUTES = 90L
private val FARMING_GUILD_BANK_TILE = Tile(1253, 3741, 0)
private val DEFAULT_MASTER_FARMER_TILE = Tile(1261, 3729, 0)
private const val DEFAULT_MASTER_FARMER_TILE_TEXT = "1261,3729,0"
private const val DEFAULT_PICKPOCKET_WINE_WITHDRAW_COUNT = 10
private const val DEFAULT_PICKPOCKET_HEAL_DEFICIT = 13
private const val DEFAULT_PICKPOCKET_DROP_LIST =
    "Jug, Potato seed, Onion seed, Cabbage seed, Tomato seed, Sweetcorn seed, Strawberry seed, Asgarnian seed, Jute seed, Yanillian seed, Krandorian seed, Wildblood seed, Redberry seed, Cadavaberry seed, Dwellberry seed"
private const val PICKPOCKET_SIDE_CHECK_INTERVAL_MS = 30_000L
private const val BANK_ACTION_RETRIES = 3
private val BIG_COMPOST_BIN_TILE = Tile(1271, 3729, 0)
private val TRACKED_MASTER_FARMER_SEED_IDS = intArrayOf(
    5100, // Limpwurt seed
    5294, // Harralander seed
    5295, // Ranarr seed
    5296, // Toadflax seed
    5297, // Irit seed
    5298, // Avantoe seed
    5299, // Kwuarm seed
    5300, // Snapdragon seed
    5301, // Cadantine seed
    5302, // Lantadyme seed
    5303, // Dwarf weed seed
    5304, // Torstol seed
    5321  // Watermelon seed
)
private object HerbRunUi {
    const val CONFIG_PAGE_OPTION = "Configuration Page"
    const val PAGE_CORE = "Core"
    const val PAGE_FALLBACK_SEEDS = "Fallback Seeds"
    const val PAGE_PICKPOCKET = "Pickpocket"
    const val PAGE_WORLD_HOPPING = "World Hopping"
    const val PAGE_PATCHES = "Patches"

    val PAGE_OPTION_GROUPS: Map<String, List<String>> = mapOf(
        PAGE_CORE to listOf(
            OPTION_HERB_TYPE,
            OPTION_COMPOST_TYPE,
            OPTION_ADDITIONAL_WITHDRAWALS,
            OPTION_REQUIRES_RAKE,
            OPTION_REQUIRES_SEED_DIBBER,
            OPTION_LOOP_RUNS,
            OPTION_NOTE_HERBS
        ),
        PAGE_FALLBACK_SEEDS to listOf(
            OPTION_FALLBACK_SEED_1,
            OPTION_FALLBACK_SEED_2,
            OPTION_FALLBACK_SEED_3,
            OPTION_FALLBACK_SEED_4,
            OPTION_FALLBACK_SEED_5
        ),
        PAGE_PICKPOCKET to listOf(
            OPTION_PICKPOCKET_BETWEEN_RUNS,
            OPTION_PICKPOCKET_ENABLE_LIMPWURT,
            OPTION_START_WITH_PICKPOCKET,
            OPTION_PICKPOCKET_WINE_WITHDRAW,
            OPTION_PICKPOCKET_HEAL_DEFICIT,
            OPTION_PICKPOCKET_DROP_LIST,
            OPTION_MASTER_FARMER_TILE
        ),
        PAGE_WORLD_HOPPING to listOf(
            OPTION_ENABLE_COOLDOWN_WORLD_HOP,
            OPTION_HOP_AFTER_EACH_RUN,
            OPTION_COOLDOWN_WORLD_HOP_MIN_MINUTES,
            OPTION_COOLDOWN_WORLD_HOP_MAX_MINUTES
        ),
        PAGE_PATCHES to listOf(
            "Run Falador Patch",
            "Run Port Phasmatys Patch",
            "Run Catherby Patch",
            "Run Ardougne Patch",
            "Run Hosidius Patch",
            "Run Troll Stronghold Patch",
            "Run Harmony Island Patch",
            "Run Weiss Patch",
            "Run Civitas Patch",
            "Run Farming Guild Patch",
            OPTION_RUN_BIG_COMPOST_BIN
        )
    )
    val ALL_PAGE_OPTIONS: Set<String> = PAGE_OPTION_GROUPS.values.flatten().toSet()
}

@ScriptManifest(
    name = "0m6 Herb Run",
    description = "Configurable herb run with patch toggles + inventory driven supplies.",
    version = "1.0.0",
    author = "0m6",
    category = org.powbot.api.script.ScriptCategory.Farming
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            name = "Instructions",
            description = "Select the herb/compost, choose which patches to visit, and optionally configure additional withdrawals.",
            optionType = OptionType.INFO
        ),
        ScriptConfiguration(
            name = HerbRunUi.CONFIG_PAGE_OPTION,
            description = "Select which configuration category to edit.",
            optionType = OptionType.STRING,
            allowedValues = [
                "",
                HerbRunUi.PAGE_CORE,
                HerbRunUi.PAGE_FALLBACK_SEEDS,
                HerbRunUi.PAGE_PICKPOCKET,
                HerbRunUi.PAGE_WORLD_HOPPING,
                HerbRunUi.PAGE_PATCHES
            ],
            defaultValue = "",
            visible = true
        ),
        ScriptConfiguration(
            name = OPTION_HERB_TYPE,
            description = "Which herb seed to plant at each patch.",
            defaultValue = "Ranarr",
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
                "Torstol",
                "Fellstalk"
            ],
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_FALLBACK_SEED_1,
            description = "First fallback herb seed type when primary runs out.",
            defaultValue = "Avantoe",
            optionType = OptionType.STRING,
            allowedValues = [
                "None",
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
                "Torstol",
                "Fellstalk"
            ],
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_FALLBACK_SEED_2,
            description = "Second fallback herb seed type when primary and first fallback run out.",
            defaultValue = "Irit",
            optionType = OptionType.STRING,
            allowedValues = [
                "None",
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
                "Torstol",
                "Fellstalk"
            ],
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_FALLBACK_SEED_3,
            description = "Third fallback herb seed type when earlier priorities run out.",
            defaultValue = "Harralander",
            optionType = OptionType.STRING,
            allowedValues = [
                "None",
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
                "Torstol",
                "Fellstalk"
            ],
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_FALLBACK_SEED_4,
            description = "Fourth fallback herb seed type when earlier priorities run out.",
            defaultValue = "Kwuarm",
            optionType = OptionType.STRING,
            allowedValues = [
                "None",
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
                "Torstol",
                "Fellstalk"
            ],
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_FALLBACK_SEED_5,
            description = "Fifth fallback herb seed type when earlier priorities run out.",
            defaultValue = "Guam",
            optionType = OptionType.STRING,
            allowedValues = [
                "None",
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
                "Torstol",
                "Fellstalk"
            ],
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_COMPOST_TYPE,
            description = "Which compost to use on each patch.",
            defaultValue = "Ultracompost",
            optionType = OptionType.STRING,
            allowedValues = [
                "Compost",
                "Supercompost",
                "Ultracompost",
                "Bottomless bucket"
            ],
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_ADDITIONAL_WITHDRAWALS,
            description = "Additional items to withdraw when banking. Format: Item:Amount, Item2:Amount",
            optionType = OptionType.STRING,
            defaultValue = "Law rune:10, Air rune:100, Fire rune:100, Earth rune:100, Water rune:100",
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_REQUIRES_RAKE,
            description = "If enabled, keep/withdraw a rake for weeding.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "true",
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_REQUIRES_SEED_DIBBER,
            description = "If enabled, keep/withdraw a seed dibber for planting.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "false",
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_LOOP_RUNS,
            description = "Restart from the first patch after completing the last enabled patch.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "true",
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_NOTE_HERBS,
            description = "Use the Tool Leprechaun to note herbs after harvesting each patch.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "true",
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_PICKPOCKET_BETWEEN_RUNS,
            description = "During cooldown between runs, pickpocket Master Farmer at Farming Guild.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "true",
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_PICKPOCKET_ENABLE_LIMPWURT,
            description = "During pickpocketing, maintain the nearby flower patch with limpwurt seeds.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "true",
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_START_WITH_PICKPOCKET,
            description = "Start in the pickpocket phase first, then begin herb runs after cooldown.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "false",
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_PICKPOCKET_WINE_WITHDRAW,
            description = "How many Jug of wine to withdraw before pickpocketing.",
            optionType = OptionType.INTEGER,
            defaultValue = DEFAULT_PICKPOCKET_WINE_WITHDRAW_COUNT.toString(),
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_PICKPOCKET_HEAL_DEFICIT,
            description = "Drink wine while stunned when HP is this much under max.",
            optionType = OptionType.INTEGER,
            defaultValue = DEFAULT_PICKPOCKET_HEAL_DEFICIT.toString(),
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_MASTER_FARMER_TILE,
            description = "Master Farmer standing tile in format x,y,plane.",
            optionType = OptionType.STRING,
            defaultValue = DEFAULT_MASTER_FARMER_TILE_TEXT,
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_PICKPOCKET_DROP_LIST,
            description = "Comma-separated item names to drop while stunned during pickpocketing.",
            optionType = OptionType.STRING,
            defaultValue = DEFAULT_PICKPOCKET_DROP_LIST,
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_ENABLE_COOLDOWN_WORLD_HOP,
            description = "Hop worlds during between-run cooldown only.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "true",
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_HOP_AFTER_EACH_RUN,
            description = "Do one blocking world hop after each herb run completes (not timer-based).",
            optionType = OptionType.BOOLEAN,
            defaultValue = "false",
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_COOLDOWN_WORLD_HOP_MIN_MINUTES,
            description = "Minimum minutes between cooldown hops.",
            optionType = OptionType.INTEGER,
            defaultValue = COOLDOWN_HOP_MIN_MINUTES.toString(),
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_COOLDOWN_WORLD_HOP_MAX_MINUTES,
            description = "Maximum minutes between cooldown hops.",
            optionType = OptionType.INTEGER,
            defaultValue = COOLDOWN_HOP_MAX_MINUTES.toString(),
            visible = false
        ),
        ScriptConfiguration(
            name = "Run Falador Patch",
            description = "Visit the south Falador allotment/herb patch.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "true",
            visible = false
        ),
        ScriptConfiguration(
            name = "Run Port Phasmatys Patch",
            description = "Visit the Port Phasmatys allotment/herb patch.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "true",
            visible = false
        ),
        ScriptConfiguration(
            name = "Run Catherby Patch",
            description = "Visit the Catherby allotment/herb patch.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "true",
            visible = false
        ),
        ScriptConfiguration(
            name = "Run Ardougne Patch",
            description = "Visit the Ardougne allotment/herb patch.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "true",
            visible = false
        ),
        ScriptConfiguration(
            name = "Run Hosidius Patch",
            description = "Visit the Hosidius allotment/herb patch.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "true",
            visible = false
        ),
        ScriptConfiguration(
            name = "Run Troll Stronghold Patch",
            description = "Visit the Troll Stronghold herb patch.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "false",
            visible = false
        ),
        ScriptConfiguration(
            name = "Run Harmony Island Patch",
            description = "Visit the Harmony Island herb patch.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "false",
            visible = false
        ),
        ScriptConfiguration(
            name = "Run Weiss Patch",
            description = "Visit the Weiss herb patch.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "false",
            visible = false
        ),
        ScriptConfiguration(
            name = "Run Civitas Patch",
            description = "Visit the Civitas illa Fortis herb patch.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "true",
            visible = false
        ),
        ScriptConfiguration(
            name = "Run Farming Guild Patch",
            description = "Visit the Farming Guild herb patch.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "true",
            visible = false
        ),
        ScriptConfiguration(
            name = OPTION_RUN_BIG_COMPOST_BIN,
            description = "Big Compost Bin in Farming Guild",
            optionType = OptionType.BOOLEAN,
            defaultValue = "true",
            visible = false
        )
    ]
)
class HerbRun : AbstractScript() {

    private lateinit var config: HerbRunConfig

    private val patchStatuses = enumValues<HerbPatch>().associateWith { HerbPatchStatus() }.toMutableMap()
    private val patchQueue = ArrayDeque<HerbPatch>()
    private var currentPatch: HerbPatch? = null
    private var patchComplete = true

    private var startTime = 0L
    private var totalHerbsHarvested = 0
    private var patchesCompletedThisRun = 0
    private var runCount = 0
    private var nextRunAtMillis = 0L
    private var startRunNowRequested = false
    private var cachedPatchContext: PatchContext? = null
    private var cachedPatchContextAt = 0L
    private var pickpocketSetupPending = false
    private var currentConfigPage = ""
    private var pickpocketBlockedUntilMillis = 0L
    private var nextPickpocketTapAtMillis = 0L
    private var lastPickpocketLimpwurtCheckAtMillis = 0L
    private var lastPickpocketHopCheckAtMillis = 0L
    private var pickpocketDropItems: List<String> = parsePickpocketDropItems(DEFAULT_PICKPOCKET_DROP_LIST)
    private var bigCompostBinPending = true
    private var lastCompletedPatch: HerbPatch? = null
    private lateinit var paintBuilder: PaintBuilder
    private var showTrackedItems = false
    private var trackedItemPaintRows: List<List<PaintItem>> = emptyList()
    private var trackedItemInsertIndex = 0
    private var showRunConfig = false
    private var runConfigPaintRows: List<List<PaintItem>> = emptyList()
    private var runConfigInsertIndex = 0
    private var enableCooldownWorldHop = true
    private var hopAfterEachRun = false
    private var pendingRunCompletionHop = false
    private var cooldownHopMinMinutes = COOLDOWN_HOP_MIN_MINUTES
    private var cooldownHopMaxMinutes = COOLDOWN_HOP_MAX_MINUTES
    private val cooldownHopScheduler = WorldHopIntervalScheduler(COOLDOWN_HOP_MIN_MINUTES, COOLDOWN_HOP_MAX_MINUTES)
    private val worldHopService = BlockingWorldHopService(
        onInfo = { logInfo(it) },
        onWarn = { logWarn(it) }
    )
    private val bankingService = HerbRunBankingService(
        defaultRetries = BANK_ACTION_RETRIES,
        logWarn = { logWarn(it) }
    )
    private val bigCompostBinService = BigCompostBinService(
        bankTile = FARMING_GUILD_BANK_TILE,
        binTile = BIG_COMPOST_BIN_TILE,
        openBankWithRetries = { reason -> openBankWithRetries(reason) },
        depositInventoryWithRetries = { depositInventoryWithRetries() },
        withdrawByNameWithRetries = { name, amount -> withdrawByNameWithRetries(name, amount) },
        isNearTile = { tile -> isNearTile(tile) },
        isNearFarmingGuildBank = { isNearFarmingGuildBank() },
        logInfo = { logInfo(it) },
        logWarn = { logWarn(it) },
        logError = { logError(it) },
        stopScript = { reason -> stopScript(reason) },
        markComplete = { bigCompostBinPending = false }
    )
    private val preflightBankingService = PreflightBankingService(this)
    private val limpwurtService = LimpwurtService(this)
    private val patchStateResolver = PatchStateResolver()
    private val configFactory = HerbRunConfigFactory { logWarn(it) }
    private val runCycleService = RunCycleService(this)
    private val uiService = HerbRunUiService(this)
    private val toolLeprechaunService = ToolLeprechaunService(this)
    private val patchProcessingService = PatchProcessingService(this)
    private val pickpocketService = PickpocketService(this)

    var preflightComplete = false
    var currentTask = "Initializing"
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    private val tasks: List<HerbRunTask> by lazy {
        listOf(
            DropDisposableItemsTask(this),
            CooldownWorldHopTask(this),
            PreflightBankTask(this),
            EnsureSuppliesTask(this),
            PickpocketTask(this),
            BigCompostBinTask(this),
            SelectPatchTask(this),
            TravelToPatchTask(this),
            WeedPatchTask(this),
            EmptyPatchTask(this),
            HarvestPatchTask(this),
            DeadPatchTask(this),
            GrowingPatchTask(this),
            UnknownPatchTask(this)
        )
    }

    internal enum class PatchState {
        NEEDS_WEEDING,
        EMPTY_SOIL,
        GROWING,
        READY_TO_HARVEST,
        DEAD,
        UNKNOWN
    }

    private data class PatchContext(
        val patch: HerbPatch,
        val patchObject: GameObject,
        val state: PatchState
    )

    override fun onStart() {
        startTime = System.currentTimeMillis()
        config = buildConfig()
        enableCooldownWorldHop = getOption(OPTION_ENABLE_COOLDOWN_WORLD_HOP)
        hopAfterEachRun = getOption(OPTION_HOP_AFTER_EACH_RUN)
        cooldownHopMinMinutes = getOption<Int>(OPTION_COOLDOWN_WORLD_HOP_MIN_MINUTES).coerceAtLeast(1)
        cooldownHopMaxMinutes = getOption<Int>(OPTION_COOLDOWN_WORLD_HOP_MAX_MINUTES).coerceAtLeast(1)
        cooldownHopScheduler.configure(cooldownHopMinMinutes, cooldownHopMaxMinutes)
        pickpocketDropItems = parsePickpocketDropItems(getOption<String>(OPTION_PICKPOCKET_DROP_LIST))
        currentConfigPage = getOption(HerbRunUi.CONFIG_PAGE_OPTION)
        updateConfigPageVisibility(currentConfigPage)

        if (config.enabledPatches.isEmpty() && !config.runBigCompostBin) {
            val reason = "No herb patches enabled. Enable at least one patch or Big Compost Bin."
            logError(reason)
            stopScript(reason)
            return
        }

        if (config.startWithPickpocket && config.pickpocketBetweenRuns && config.loopRuns) {
            patchQueue.clear()
            runCount = 0
            preflightComplete = true
            nextRunAtMillis = System.currentTimeMillis() + cooldownMillis()
            pickpocketSetupPending = true
            bigCompostBinPending = false
            lastCompletedPatch = null
            currentTask = "Pickpocket cooldown"
            logInfo("Starting in pickpocket phase before first herb run.")
        } else {
            patchQueue.addAll(config.enabledPatches)
            runCount = 1
            preflightComplete = false
            bigCompostBinPending = config.runBigCompostBin
            lastCompletedPatch = null
            if (config.startWithPickpocket) {
                logWarn("Start pickpocketing requires Loop Runs + Pickpocket Between Runs enabled. Starting with herb run.")
            }
        }
        cooldownHopScheduler.scheduleNext()

        paintBuilder = PaintBuilder.newBuilder()
            .x(40).y(80)
            .addString("Task") { currentTask }
            .addString("Patch") { currentPatch?.displayName ?: "-" }
            .addString("Run") { runCount.toString() }
            .addString("Patches (run)") { "$patchesCompletedThisRun/${config.enabledPatches.size}" }
            .addString("Herbs Harvested") { totalHerbsHarvested.toString() }
            .addString("Cooldown") { cooldownRemainingText() }
            .addCheckbox("Start run now", START_RUN_NOW_CHECKBOX_ID, false)
            .addCheckbox("Show run config", SHOW_RUN_CONFIG_CHECKBOX_ID, showRunConfig)
            .addCheckbox("Show tracked items", SHOW_TRACKED_ITEMS_CHECKBOX_ID, showTrackedItems)
        val configPatchLines = if (config.enabledPatches.isEmpty()) {
            listOf("None")
        } else {
            config.enabledPatches
                .map { it.displayName }
                .chunked(2)
                .map { it.joinToString(", ") }
        }
        runConfigInsertIndex = paintBuilder.items.size
        val runConfigRowsStart = paintBuilder.items.size
        paintBuilder
            .addString("Config Seed") { config.seedItemName }
            .addString("Config Seed Fallbacks") {
                config.fallbackSeedItemNames.joinToString(", ").ifBlank { "None" }
            }
            .addString("Config Compost") { config.compostItemName ?: "None" }
            .addString("Config Compost Bin") { if (config.runBigCompostBin) "Yes" else "No" }
            .addString("Config Limpwurt") { if (config.enableLimpwurtFarming) "Yes" else "No" }
        configPatchLines.forEachIndexed { index, line ->
            val label = if (index == 0) "Config Patches" else "Config Patches ${index + 1}"
            paintBuilder.addString(label) { line }
        }
        runConfigPaintRows = paintBuilder.items.subList(runConfigRowsStart, paintBuilder.items.size).toList()
        if (!showRunConfig) {
            toggleRunConfig(show = false)
        }
        trackedItemInsertIndex = paintBuilder.items.size
        val trackedRowsStart = paintBuilder.items.size
        paintBuilder.trackInventoryItems(*TRACKED_MASTER_FARMER_SEED_IDS)
        trackedItemPaintRows = paintBuilder.items.subList(trackedRowsStart, paintBuilder.items.size).toList()
        if (!showTrackedItems) {
            toggleTrackedItems(show = false)
        }
        val paint = paintBuilder
            .trackSkill(Skill.Farming)
            .trackSkill(Skill.Thieving)
            .build()
        addPaint(paint)

        logInfo(
            "Herb run initialized -> Herb ${config.herbType.displayName}, " +
                "Seed Priority: ${config.seedPriorityItemNames.joinToString(" -> ")}, " +
                "Compost: ${config.compostItemName ?: "None"}"
        )
        logInfo("Enabled patches: ${config.enabledPatches.joinToString { it.displayName }}")
    }

    override fun poll() {
        try {
            for (task in tasks) {
                if (task.shouldExecute()) {
                    currentTask = task.taskName
                    logInfo("Task: ${task.taskName}")
                    task.execute()
                    return
                }
            }
            currentTask = "Idle"
            Condition.sleep(Random.nextInt(400, 700))
        } catch (e: Exception) {
            logError("Error in poll: ${e.message}", e)
            Condition.sleep(600)
        }
    }

    override fun canBreak(): Boolean {
        val atBankTile = isNearTile(FARMING_GUILD_BANK_TILE)
        val atMasterFarmerWhileStunned =
            isNearTile(config.pickpocketMasterFarmerTile) && Players.local().animation() == 415
        val safeBreak = atBankTile || atMasterFarmerWhileStunned
        if (!safeBreak && ScriptManager.isBreakRequested()) {
            logInfo("BREAK: requested but waiting for Farming Guild bank tile or Master Farmer stun window.")
        }
        return safeBreak
    }

    /* === Task helpers === */

    fun shouldCheckSupplies(): Boolean {
        return !preflightComplete && config.enabledPatches.isNotEmpty()
    }

    fun shouldHandleBigCompostBin(): Boolean {
        if (!config.runBigCompostBin) {
            return false
        }
        if (!bigCompostBinPending) {
            return false
        }
        if (runCount <= 0) {
            return false
        }
        if (currentPatch != null || !patchComplete || patchQueue.isNotEmpty()) {
            return false
        }
        return patchesCompletedThisRun >= config.enabledPatches.size
    }

    fun handleBigCompostBinPhase() {
        currentTask = "Big compost bin"
        bigCompostBinService.runPhase()
    }

    fun shouldPickpocketBetweenRuns(): Boolean {
        if (!config.loopRuns || !config.pickpocketBetweenRuns) {
            return false
        }
        return isInCooldownPhase()
    }

    fun shouldDropDisposableItems(): Boolean {
        return Inventory.stream().name("Bucket", "Weeds").isNotEmpty()
    }

    fun dropDisposableItems() {
        dropItems()
    }

    fun shouldHandleCooldownWorldHopTask(): Boolean {
        val now = System.currentTimeMillis()
        if (shouldPickpocketBetweenRuns() && now - lastPickpocketHopCheckAtMillis < PICKPOCKET_SIDE_CHECK_INTERVAL_MS) {
            return false
        }
        if (pendingRunCompletionHop && isInCooldownPhase()) {
            return true
        }
        if (!enableCooldownWorldHop) {
            return false
        }
        if (!isInCooldownPhase()) {
            return false
        }
        return cooldownHopScheduler.shouldHopNow()
    }

    fun handleCooldownWorldHopTask() {
        handleCooldownWorldHop()
    }

    internal fun limpwurtMasterFarmerTile(): Tile = config.pickpocketMasterFarmerTile
    internal fun limpwurtCompostName(): String? = config.compostItemName
    internal fun limpwurtSetTask(taskName: String) {
        currentTask = taskName
    }
    internal fun limpwurtHasObjectAction(obj: GameObject, action: String): Boolean = hasObjectAction(obj, action)
    internal fun limpwurtApplyCompost(patchObject: GameObject): Boolean = applyCompost(patchObject)
    internal fun limpwurtIsNearTile(tile: Tile): Boolean = isNearTile(tile)
    internal fun limpwurtBankTile(): Tile = FARMING_GUILD_BANK_TILE
    internal fun limpwurtIsNearGuildBank(): Boolean = isNearFarmingGuildBank()
    internal fun limpwurtOpenBank(reason: String): Boolean = openBankWithRetries(reason)
    internal fun limpwurtDepositInventory(): Boolean = depositInventoryWithRetries()
    internal fun limpwurtWithdraw(name: String, amount: Int): Boolean = withdrawByNameWithRetries(name, amount)
    internal fun limpwurtInfo(message: String) = logInfo(message)
    internal fun limpwurtWarn(message: String) = logWarn(message)
    internal fun limpwurtError(message: String) = logError(message)
    internal fun limpwurtStop(reason: String) = stopScript(reason)
    internal fun preflightConfig(): HerbRunConfig = config
    internal fun preflightBankTile(): Tile = FARMING_GUILD_BANK_TILE
    internal fun preflightIsNearTile(tile: Tile): Boolean = isNearTile(tile)
    internal fun preflightIsNearGuildBank(): Boolean = isNearFarmingGuildBank()
    internal fun preflightOpenBank(reason: String): Boolean = openBankWithRetries(reason)
    internal fun preflightDepositInventory(): Boolean = depositInventoryWithRetries()
    internal fun preflightWithdrawById(itemId: Int, inventoryCheck: () -> Int, targetIncrease: Int): Boolean =
        withdrawByIdWithRetries(itemId, inventoryCheck, targetIncrease)
    internal fun preflightRequiresRake(): Boolean = getOption(OPTION_REQUIRES_RAKE)
    internal fun preflightRequiresSeedDibber(): Boolean = getOption(OPTION_REQUIRES_SEED_DIBBER)
    internal fun preflightAdditionalWithdrawalsRaw(): String =
        getOption<String>(OPTION_ADDITIONAL_WITHDRAWALS).trim()
    internal fun preflightSetComplete() {
        preflightComplete = true
    }
    internal fun preflightInfo(message: String) = logInfo(message)
    internal fun preflightWarn(message: String) = logWarn(message)
    internal fun preflightError(message: String) = logError(message)
    internal fun preflightStop(reason: String) = stopScript(reason)

    private fun isInCooldownPhase(): Boolean {
        if (!config.loopRuns) {
            return false
        }
        if (currentPatch != null || !patchComplete || patchQueue.isNotEmpty()) {
            return false
        }
        if (startRunNowRequested) {
            return false
        }
        return nextRunAtMillis > System.currentTimeMillis()
    }

    private fun handleCooldownWorldHop(): Boolean {
        // Never hop during active patch processing. Hops are cooldown-only.
        val now = System.currentTimeMillis()
        if (shouldPickpocketBetweenRuns()) {
            if (now - lastPickpocketHopCheckAtMillis < PICKPOCKET_SIDE_CHECK_INTERVAL_MS) {
                return false
            }
            lastPickpocketHopCheckAtMillis = now
        }
        if (pendingRunCompletionHop && isInCooldownPhase()) {
            currentTask = "Run completion world hop"
            worldHopService.hopToPreferredWorldWithRetries("Herb run completion hop")
            pendingRunCompletionHop = false
            cooldownHopScheduler.scheduleNext()
            return true
        }
        if (!enableCooldownWorldHop) {
            return false
        }
        if (!isInCooldownPhase()) {
            return false
        }
        if (!cooldownHopScheduler.shouldHopNow()) {
            return false
        }
        currentTask = "Cooldown world hop"
        worldHopService.hopToPreferredWorldWithRetries("Herb run cooldown hop")
        cooldownHopScheduler.scheduleNext()
        return true
    }

    fun performBetweenRunPickpocketing() {
        pickpocketService.performBetweenRunPickpocketing()
    }

    internal fun pickpocketBlockedUntilMillis(): Long = pickpocketBlockedUntilMillis
    internal fun pickpocketSetBlockedUntilMillis(value: Long) {
        pickpocketBlockedUntilMillis = value
    }
    internal fun pickpocketNextTapAtMillis(): Long = nextPickpocketTapAtMillis
    internal fun pickpocketSetNextTapAtMillis(value: Long) {
        nextPickpocketTapAtMillis = value
    }
    internal fun pickpocketSetupPending(): Boolean = pickpocketSetupPending
    internal fun pickpocketSetSetupPending(value: Boolean) {
        pickpocketSetupPending = value
    }
    internal fun pickpocketLastLimpwurtCheckAtMillis(): Long = lastPickpocketLimpwurtCheckAtMillis
    internal fun pickpocketSetLastLimpwurtCheckAtMillis(value: Long) {
        lastPickpocketLimpwurtCheckAtMillis = value
    }
    internal fun pickpocketEnableLimpwurtFarming(): Boolean = config.enableLimpwurtFarming
    internal fun pickpocketHandleLimpwurt(): Boolean = limpwurtService.handleDuringPickpocket()
    internal fun pickpocketMasterFarmerTile(): Tile = config.pickpocketMasterFarmerTile
    internal fun pickpocketSetCurrentTask(task: String) {
        currentTask = task
    }
    internal fun pickpocketDebug(message: String) = logDebug(message)
    internal fun pickpocketInfo(message: String) = logInfo(message)
    internal fun pickpocketWarn(message: String) = logWarn(message)
    internal fun pickpocketError(message: String) = logError(message)
    internal fun pickpocketStop(reason: String) = stopScript(reason)
    internal fun pickpocketIsNearTile(tile: Tile): Boolean = isNearTile(tile)
    internal fun pickpocketBankTile(): Tile = FARMING_GUILD_BANK_TILE
    internal fun pickpocketOpenBank(reason: String): Boolean = openBankWithRetries(reason)
    internal fun pickpocketIsNearGuildBank(): Boolean = isNearFarmingGuildBank()
    internal fun pickpocketDepositInventory(): Boolean = depositInventoryWithRetries()
    internal fun pickpocketWithdrawByName(name: String, amount: Int): Boolean = withdrawByNameWithRetries(name, amount)
    internal fun pickpocketWineWithdrawAmount(): Int = config.pickpocketWineWithdrawAmount
    internal fun pickpocketHealHpDeficit(): Int = config.pickpocketHealHpDeficit
    internal fun pickpocketDropItems(): Array<String> = pickpocketDropItems.toTypedArray()

    fun hasRequiredSupplies(): Boolean {
        val requiredSeeds = config.enabledPatches.size
        val availableSeeds = config.seedPriorityItemNames.sumOf { Inventory.stream().name(it).count(true).toInt() }
        val hasSeeds = availableSeeds >= requiredSeeds
        val compostOk = if (config.hasCompostName) {
            val compostName = config.compostItemName
            val requiredCompost = if (compostName != null && compostName.contains("bottomless", ignoreCase = true)) {
                1
            } else {
                config.enabledPatches.size
            }
            Inventory.stream().name(compostName).count(true).toInt() >= requiredCompost
        } else {
            true
        }
        return hasSeeds && compostOk
    }

    fun handleMissingSupplies() {
        val seedPriority = config.seedPriorityItemNames
        val seedLabel = if (seedPriority.isEmpty()) {
            "seed"
        } else {
            seedPriority.joinToString(" / ")
        }
        val compostName = config.compostItemName
        val messages = mutableListOf<String>()
        val availableSeeds = seedPriority.sumOf { Inventory.stream().name(it).count(true).toInt() }
        if (availableSeeds <= 0) {
            messages.add("herb seeds ($seedLabel)")
        }
        if (config.hasCompostName && (compostName == null || Inventory.stream().name(compostName).count(true) == 0L)) {
            messages.add("compost (${compostName ?: "unknown"})")
        }
        val reason = if (messages.isEmpty()) {
            "Missing supplies"
        } else {
            "Missing ${messages.joinToString()} in inventory."
        }
        logError(reason)
        stopScript(reason)
    }

    fun needsNextPatch(): Boolean {
        return currentPatch == null || patchComplete
    }

    fun selectNextPatch() {
        runCycleService.selectNextPatch()
    }

    fun shouldTravelToPatch(): Boolean {
        val patch = currentPatch ?: return false
        return !patchComplete && !isNearPatch(patch)
    }

    fun travelToActivePatch() {
        val patch = currentPatch ?: return
        MovementUtils.enableRunIfNeeded()
        logInfo("Walking to patch: ${patch.displayName}")
        Movement.walkTo(patch.tile)
        val reached = Condition.wait({ isNearPatch(patch) }, 300, 20)
        if (!reached) {
            logWarn("Failed to reach ${patch.displayName} yet, retrying...")
        } else {
            logInfo("Arrived near ${patch.displayName}")
        }
    }

    fun shouldProcessCurrentPatch(): Boolean {
        val patch = currentPatch ?: return false
        return !patchComplete && isNearPatch(patch)
    }

    fun shouldWeedPatch(): Boolean = getPatchContext()?.state == PatchState.NEEDS_WEEDING

    fun weedCurrentPatch() {
        val ctx = getPatchContext() ?: return
        handleWeeding(ctx.patch, ctx.patchObject)
        invalidatePatchContext()
    }

    fun shouldHandleEmptyPatch(): Boolean = getPatchContext()?.state == PatchState.EMPTY_SOIL

    fun handleEmptyPatchTask() {
        val ctx = getPatchContext() ?: return
        handleEmptyPatch(ctx.patch, ctx.patchObject)
        invalidatePatchContext()
    }

    fun shouldHarvestPatch(): Boolean = getPatchContext()?.state == PatchState.READY_TO_HARVEST

    fun harvestCurrentPatch() {
        val ctx = getPatchContext() ?: return
        harvestPatch(ctx.patch, ctx.patchObject)
        val refreshed = findPatchObject(ctx.patch)
        if (refreshed != GameObject.Nil) {
            val refreshedState = determinePatchState(refreshed)
            if (refreshedState == PatchState.EMPTY_SOIL) {
                handleEmptyPatch(ctx.patch, refreshed)
            }
        }
        invalidatePatchContext()
    }

    fun shouldClearDeadPatch(): Boolean = getPatchContext()?.state == PatchState.DEAD

    fun clearDeadPatchTask() {
        val ctx = getPatchContext() ?: return
        clearDeadPatch(ctx.patch, ctx.patchObject)
        val refreshed = findPatchObject(ctx.patch)
        if (refreshed != GameObject.Nil) {
            val refreshedState = determinePatchState(refreshed)
            if (refreshedState == PatchState.EMPTY_SOIL) {
                handleEmptyPatch(ctx.patch, refreshed)
            }
        }
        invalidatePatchContext()
    }

    fun shouldHandleGrowingPatch(): Boolean = getPatchContext()?.state == PatchState.GROWING

    fun handleGrowingPatchTask() {
        val ctx = getPatchContext() ?: return
        val status = patchStatuses[ctx.patch]
        if (status?.seedPlanted == true) {
            logInfo("${ctx.patch.displayName}: Fresh herbs planted. Moving to next patch.")
        } else {
            logInfo("${ctx.patch.displayName}: Herbs still growing. Nothing to do.")
        }
        finishPatch(ctx.patch)
        invalidatePatchContext()
    }

    fun shouldHandleUnknownPatch(): Boolean = getPatchContext()?.state == PatchState.UNKNOWN

    fun handleUnknownPatchTask() {
        val ctx = getPatchContext() ?: return
        logDebug("${ctx.patch.displayName}: Patch state unknown, waiting...")
        Condition.sleep(400)
        invalidatePatchContext()
    }

    private fun getPatchContext(): PatchContext? {
        if (!shouldProcessCurrentPatch()) return null
        val now = System.currentTimeMillis()
        if (now - cachedPatchContextAt < 200L) {
            return cachedPatchContext
        }
        cachedPatchContextAt = now
        cachedPatchContext = buildPatchContext()
        return cachedPatchContext
    }

    private fun invalidatePatchContext() {
        cachedPatchContextAt = 0L
        cachedPatchContext = null
    }

    private fun buildPatchContext(): PatchContext? {
        val patch = currentPatch ?: return null
        if (!ensureToolsAtPatch(patch)) {
            return null
        }
        val patchObject = findPatchObject(patch)
        if (patchObject == GameObject.Nil) {
            val status = patchStatuses[patch]
            if (status != null) {
                status.missingCount += 1
                if (status.missingCount >= 3) {
                    logWarn("${patch.displayName}: Patch object not found 3 times. Skipping patch.")
                    finishPatch(patch)
                    return null
                }
            }
            logWarn("Unable to find herb patch object at ${patch.displayName}, searching again...")
            Condition.sleep(400)
            return null
        }
        patchStatuses[patch]?.missingCount = 0
        val state = determinePatchState(patchObject)
        logInfo("${patch.displayName}: Patch state -> $state")
        return PatchContext(patch, patchObject, state)
    }

    /* === Patch logic === */

    private fun handleWeeding(patch: HerbPatch, patchObject: GameObject) {
        patchProcessingService.handleWeeding(patch, patchObject)
    }

    private fun handleEmptyPatch(patch: HerbPatch, patchObject: GameObject) {
        patchProcessingService.handleEmptyPatch(patch, patchObject)
    }

    private fun applyCompost(patchObject: GameObject): Boolean {
        val compostName = config.compostItemName ?: return true
        val compostItem = Inventory.stream().name(compostName).firstOrNull()
        if (compostItem == null) {
            logWarn("Missing compost: $compostName")
            return false
        }

        logInfo("Using compost: $compostName")
        if (!compostItem.interact("Use")) {
            return false
        }
        Condition.sleep(Random.nextInt(200, 350))

        logInfo("Applying compost to patch.")
        if (!patchObject.interact("Use")) {
            return false
        }

        Condition.wait(
            { Players.local().animation() != -1 },
            200,
            75
        )
        Condition.wait(
            { Players.local().animation() == -1 },
            200,
            75
        )
        logInfo("Applied $compostName")
        return true
    }

    private fun harvestPatch(patch: HerbPatch, patchObject: GameObject) {
        patchProcessingService.harvestPatch(patch, patchObject)
    }

    private fun noteHerbs(patch: HerbPatch) {
        toolLeprechaunService.noteHerbs(patch)
    }

    private fun ensureToolsAtPatch(patch: HerbPatch): Boolean {
        return toolLeprechaunService.ensureToolsAtPatch(patch)
    }

    internal fun toolDesiredCompostName(): String? = config.compostItemName
    internal fun toolRequiresRake(): Boolean = getOption(OPTION_REQUIRES_RAKE)
    internal fun toolRequiresSeedDibber(): Boolean = getOption(OPTION_REQUIRES_SEED_DIBBER)
    internal fun toolInfo(message: String) = logInfo(message)
    internal fun toolWarn(message: String) = logWarn(message)


    private fun clearDeadPatch(patch: HerbPatch, patchObject: GameObject) {
        patchProcessingService.clearDeadPatch(patch, patchObject)
    }

    internal fun patchStatus(patch: HerbPatch) = patchStatuses[patch]
    internal fun patchResetStatus(patch: HerbPatch) {
        patchStatuses[patch]?.reset()
    }
    internal fun patchFindPatchObject(patch: HerbPatch): GameObject = findPatchObject(patch)
    internal fun patchDetermineState(patchObject: GameObject): PatchState = determinePatchState(patchObject)
    internal fun patchInfo(message: String) = logInfo(message)
    internal fun patchWarn(message: String) = logWarn(message)
    internal fun patchDebug(message: String) = logDebug(message)
    internal fun patchError(message: String) = logError(message)
    internal fun patchFinishPatch(patch: HerbPatch) = finishPatch(patch)
    internal fun patchHasCompostName(): Boolean = config.hasCompostName
    internal fun patchCompostItemName(): String? = config.compostItemName
    internal fun patchSeedPriorityItemNames(): List<String> = config.seedPriorityItemNames
    internal fun patchProduceName(): String = config.herbType.produceName
    internal fun patchShouldNoteHarvest(): Boolean = config.noteHarvest
    internal fun patchNoteHerbs(patch: HerbPatch) = noteHerbs(patch)
    internal fun patchAddHerbsHarvested(amount: Int) {
        totalHerbsHarvested += amount
    }

    private fun finishPatch(patch: HerbPatch) {
        patchStatuses[patch]?.reset()
        lastCompletedPatch = patch
        patchesCompletedThisRun++
        patchComplete = true
        currentPatch = null
    }

    private fun findPatchObject(patch: HerbPatch?): GameObject {
        if (patch == null) return GameObject.Nil
        patch.herbPatchObjectId?.let { objectId ->
            val byId = Objects.stream()
                .id(objectId)
                .within(patch.tile, 14.0)
                .nearest()
                .firstOrNull()
            if (byId != null && byId.valid()) {
                return byId
            }
        }
        val targetNames = arrayOf(
            "Herb patch",
            "${config.herbType.displayName} patch",
            config.herbType.displayName,
            "Herbs",
            "Dead herbs"
        )
        return Objects.stream()
            .name(*targetNames)
            .within(patch.tile, 14.0)
            .nearest()
            .firstOrNull() ?: GameObject.Nil
    }

    private fun determinePatchState(patchObject: GameObject): PatchState {
        return patchStateResolver.resolve(patchObject)
    }

    private fun restartRunCycle(reasonPrefix: String) {
        runCycleService.restartRunCycle(reasonPrefix)
    }

    internal fun runCyclePatchQueueIsEmpty(): Boolean = patchQueue.isEmpty()
    internal fun runCycleLoopRuns(): Boolean = config.loopRuns
    internal fun runCycleInfo(message: String) = logInfo(message)
    internal fun runCycleStop(reason: String) = stopScript(reason)
    internal fun runCycleStartRunNowRequested(): Boolean = startRunNowRequested
    internal fun runCycleClearStartRunNowRequested() {
        startRunNowRequested = false
    }
    internal fun runCycleSetNextRunAt(value: Long) {
        nextRunAtMillis = value
    }
    internal fun runCycleNextRunAt(): Long = nextRunAtMillis
    internal fun runCycleCooldownMillis(): Long = cooldownMillis()
    internal fun runCycleCooldownMinutes(): Long = cooldownMinutes()
    internal fun runCycleSetPendingRunCompletionHop(value: Boolean) {
        pendingRunCompletionHop = value
    }
    internal fun runCycleHopAfterEachRun(): Boolean = hopAfterEachRun
    internal fun runCycleSetCurrentTask(task: String) {
        currentTask = task
    }
    internal fun runCyclePopNextPatch(): HerbPatch = patchQueue.removeFirst()
    internal fun runCycleSetCurrentPatch(patch: HerbPatch?) {
        currentPatch = patch
    }
    internal fun runCycleSetPatchComplete(value: Boolean) {
        patchComplete = value
    }
    internal fun runCycleSetPreflightComplete(value: Boolean) {
        preflightComplete = value
    }
    internal fun runCycleEnqueueEnabledPatches() {
        patchQueue.addAll(config.enabledPatches)
    }
    internal fun runCycleSetPatchesCompletedThisRun(value: Int) {
        patchesCompletedThisRun = value
    }
    internal fun runCycleSetBigCompostBinPending(value: Boolean) {
        bigCompostBinPending = value
    }
    internal fun runCycleIncrementRunCount() {
        runCount++
    }
    internal fun runCycleRunCount(): Int = runCount

    private fun cooldownMinutes(): Long {
        return if (config.runBigCompostBin) COOLDOWN_WITH_COMPOST_BIN_MINUTES else COOLDOWN_WITHOUT_COMPOST_BIN_MINUTES
    }

    private fun cooldownMillis(): Long = cooldownMinutes() * 60L * 1000L

    private fun cooldownRemainingText(): String {
        if (!config.loopRuns) {
            return "-"
        }
        val remaining = (nextRunAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        if (nextRunAtMillis == 0L || remaining == 0L) {
            return "-"
        }
        val totalSeconds = remaining / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format("%d:%02d:%02d", hours, minutes, seconds)
    }

    private fun isNearPatch(patch: HerbPatch): Boolean {
        val playerTile = Players.local().tile()
        return distanceBetween(playerTile, patch.tile) <= 10
    }

    private fun isNearTile(tile: Tile): Boolean {
        val playerTile = Players.local().tile()
        return distanceBetween(playerTile, tile) <= 10
    }

    private fun isNearFarmingGuildBank(): Boolean {
        if (!isNearTile(FARMING_GUILD_BANK_TILE)) {
            return false
        }
        val bankEntity = Bank.getBank()
        if (!bankEntity.valid()) {
            return false
        }
        return bankEntity.distance() <= 10
    }


    private fun hasObjectAction(obj: GameObject, action: String): Boolean {
        val actions = obj.actions()
        return actions.any { it.equals(action, ignoreCase = true) }
    }


    private fun openBankWithRetries(reason: String, attempts: Int = BANK_ACTION_RETRIES): Boolean {
        return bankingService.openBankWithRetries(reason, attempts)
    }

    private fun depositInventoryWithRetries(attempts: Int = BANK_ACTION_RETRIES): Boolean {
        return bankingService.depositInventoryWithRetries(attempts)
    }

    private fun withdrawByNameWithRetries(name: String, amount: Int, attempts: Int = BANK_ACTION_RETRIES): Boolean {
        return bankingService.withdrawByNameWithRetries(name, amount, attempts)
    }

    private fun withdrawByIdWithRetries(
        itemId: Int,
        inventoryCheck: () -> Int,
        targetIncrease: Int,
        attempts: Int = BANK_ACTION_RETRIES
    ): Boolean {
        return bankingService.withdrawByIdWithRetries(itemId, inventoryCheck, targetIncrease, attempts)
    }

    @Subscribe
    @Suppress("unused")
    fun onPaintCheckboxChanged(evt: PaintCheckboxChangedEvent) {
        uiService.onPaintCheckboxChanged(evt)
    }

    private fun toggleTrackedItems(show: Boolean) {
        uiService.toggleTrackedItems(show)
    }

    private fun toggleRunConfig(show: Boolean) {
        uiService.toggleRunConfig(show)
    }

    @ValueChanged(HerbRunUi.CONFIG_PAGE_OPTION)
    fun onConfigurationPageChanged(page: String) {
        updateConfigPageVisibility(page)
    }

    private fun distanceBetween(first: Tile, second: Tile): Int {
        return abs(first.x() - second.x()).coerceAtLeast(abs(first.y() - second.y()))
    }

    private fun dropItems(): Boolean {
        var droppedAny = false
        while (true) {
            val item = Inventory.stream().name("Bucket", "Weeds").firstOrNull() ?: break
            val itemName = item.name()
            val beforeCount = Inventory.stream().name(itemName).count(true).toInt()
            logInfo("Dropping ${item.name()}.")
            if (!item.interact("Drop")) {
                break
            }
            droppedAny = true
            Condition.wait(
                { Inventory.stream().name(itemName).count(true).toInt() < beforeCount },
                200,
                10
            )
        }
        return droppedAny
    }

    fun performPreflightBanking() {
        preflightBankingService.performPreflightBanking()
    }

    private fun updateConfigPageVisibility(page: String) {
        uiService.updateConfigPageVisibility(page)
    }

    internal fun uiStartRunNowCheckboxId(): String = START_RUN_NOW_CHECKBOX_ID
    internal fun uiShowTrackedItemsCheckboxId(): String = SHOW_TRACKED_ITEMS_CHECKBOX_ID
    internal fun uiShowRunConfigCheckboxId(): String = SHOW_RUN_CONFIG_CHECKBOX_ID
    internal fun uiSetStartRunNowRequested(value: Boolean) {
        startRunNowRequested = value
    }
    internal fun uiInfo(message: String) = logInfo(message)
    internal fun uiSetShowTrackedItems(show: Boolean) {
        showTrackedItems = show
    }
    internal fun uiSetShowRunConfig(show: Boolean) {
        showRunConfig = show
    }
    internal fun uiPaintBuilderInitialized(): Boolean = ::paintBuilder.isInitialized
    internal fun uiPaintItems(): MutableList<List<PaintItem>> = paintBuilder.items
    internal fun uiTrackedItemPaintRows(): List<List<PaintItem>> = trackedItemPaintRows
    internal fun uiTrackedItemInsertIndex(): Int = trackedItemInsertIndex
    internal fun uiRunConfigPaintRows(): List<List<PaintItem>> = runConfigPaintRows
    internal fun uiRunConfigInsertIndex(): Int = runConfigInsertIndex
    internal fun uiEnsureTrackedItemRows() {
        if (!::paintBuilder.isInitialized || trackedItemPaintRows.isNotEmpty()) {
            return
        }
        val start = paintBuilder.items.size
        paintBuilder.trackInventoryItems(*TRACKED_MASTER_FARMER_SEED_IDS)
        trackedItemInsertIndex = start
        trackedItemPaintRows = paintBuilder.items.subList(start, paintBuilder.items.size).toList()
    }
    internal fun uiSetCurrentConfigPage(page: String) {
        currentConfigPage = page
    }
    internal fun uiPageVisibleSet(page: String): Set<String> = HerbRunUi.PAGE_OPTION_GROUPS[page]?.toSet() ?: emptySet()
    internal fun uiAllPageOptions(): Set<String> = HerbRunUi.ALL_PAGE_OPTIONS
    internal fun uiUpdateOptionVisibility(optionName: String, visible: Boolean) {
        updateVisibility(optionName, visible)
    }


    private fun buildConfig(): HerbRunConfig {
        return configFactory.build(
            keys = HerbRunConfigFactory.Keys(
                herbType = OPTION_HERB_TYPE,
                fallbackSeed1 = OPTION_FALLBACK_SEED_1,
                fallbackSeed2 = OPTION_FALLBACK_SEED_2,
                fallbackSeed3 = OPTION_FALLBACK_SEED_3,
                fallbackSeed4 = OPTION_FALLBACK_SEED_4,
                fallbackSeed5 = OPTION_FALLBACK_SEED_5,
                compostType = OPTION_COMPOST_TYPE,
                runBigCompostBin = OPTION_RUN_BIG_COMPOST_BIN,
                loopRuns = OPTION_LOOP_RUNS,
                noteHerbs = OPTION_NOTE_HERBS,
                pickpocketBetweenRuns = OPTION_PICKPOCKET_BETWEEN_RUNS,
                enableLimpwurtFarming = OPTION_PICKPOCKET_ENABLE_LIMPWURT,
                startWithPickpocket = OPTION_START_WITH_PICKPOCKET,
                pickpocketWineWithdraw = OPTION_PICKPOCKET_WINE_WITHDRAW,
                pickpocketHealDeficit = OPTION_PICKPOCKET_HEAL_DEFICIT,
                masterFarmerTile = OPTION_MASTER_FARMER_TILE
            ),
            getString = { getOption(it) },
            getBoolean = { getOption(it) },
            getInt = { getOption(it) },
            defaultMasterFarmerTile = DEFAULT_MASTER_FARMER_TILE,
            defaultPickpocketWineWithdrawCount = DEFAULT_PICKPOCKET_WINE_WITHDRAW_COUNT,
            defaultPickpocketHealDeficit = DEFAULT_PICKPOCKET_HEAL_DEFICIT
        )
    }

    private fun parsePickpocketDropItems(raw: String?): List<String> {
        val source = raw?.trim().orEmpty()
        val parsed = source
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        if (parsed.isNotEmpty()) {
            return parsed
        }
        return DEFAULT_PICKPOCKET_DROP_LIST
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun logInfo(message: String) {
        logger.info("{${LocalTime.now().format(timeFormatter)}} $message")
    }

    private fun logWarn(message: String) {
        logger.warn("{${LocalTime.now().format(timeFormatter)}} $message")
    }

    private fun logError(message: String, throwable: Throwable? = null) {
        if (throwable == null) {
            logger.error("{${LocalTime.now().format(timeFormatter)}} $message")
        } else {
            logger.error("{${LocalTime.now().format(timeFormatter)}} $message", throwable)
        }
    }

    private fun stopScript(reason: String) {
        Notifications.showNotification(reason)
        ScriptManager.stop()
    }

    private fun logDebug(message: String) {
        logger.debug("{${LocalTime.now().format(timeFormatter)}} $message")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            HerbRun().startScript("localhost", "0m6", false)
        }
    }
}


