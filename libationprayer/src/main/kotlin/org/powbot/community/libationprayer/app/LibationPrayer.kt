package org.powbot.community.libationprayer.app

import org.powbot.api.Condition
import org.powbot.api.script.AbstractScript
import org.powbot.api.script.OptionType
import org.powbot.api.script.ScriptCategory
import org.powbot.api.script.ScriptConfiguration
import org.powbot.api.script.ScriptManifest
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.community.api.ScriptLogging
import org.powbot.community.libationprayer.client.LibationGameClient
import org.powbot.community.libationprayer.domain.decision.LibationAction
import org.powbot.community.libationprayer.domain.decision.LibationDecisionService
import org.powbot.community.libationprayer.domain.model.LibationConfig
import org.powbot.community.libationprayer.domain.model.LibationRuntime
import org.powbot.community.libationprayer.exec.BankingExecutor
import org.powbot.community.libationprayer.exec.BlessingExecutor
import org.powbot.community.libationprayer.exec.PrayerRechargeExecutor
import org.powbot.community.libationprayer.exec.SacrificeExecutor
import org.powbot.community.libationprayer.util.LibationConstants

@ScriptManifest(
    name = "0m6 Libation Prayer",
    description = "Banks wine and bone shards, blesses at Exposed altar, and sacrifices at Libation bowl.",
    version = "1.0.0",
    author = "0m6",
    category = ScriptCategory.Prayer
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            name = "Info",
            description = "Starts at Aubervale flow. Banks at 1416,3356,0 then blesses and sacrifices.",
            optionType = OptionType.INFO
        ),
        ScriptConfiguration(
            name = "Wine Type",
            description = "Select which jug to withdraw and bless.",
            optionType = OptionType.STRING,
            defaultValue = "Jug of wine",
            allowedValues = ["Jug of wine", "Jug of sunfire wine"]
        ),
        ScriptConfiguration(
            name = "Minimum Bone Shards",
            description = "Stop if inventory has fewer than this after banking.",
            optionType = OptionType.INTEGER,
            defaultValue = "100"
        )
    ]
)
class LibationPrayer : AbstractScript() {
    private lateinit var config: LibationConfig
    private val runtime = LibationRuntime()

    private lateinit var client: LibationGameClient
    private lateinit var decisionService: LibationDecisionService
    private lateinit var bankingExecutor: BankingExecutor
    private lateinit var blessingExecutor: BlessingExecutor
    private lateinit var prayerRechargeExecutor: PrayerRechargeExecutor
    private lateinit var sacrificeExecutor: SacrificeExecutor

    override fun onStart() {
        config = LibationConfig(
            wineType = getOption<String>("Wine Type"),
            minimumBoneShards = getOption<Int>("Minimum Bone Shards").coerceAtLeast(1)
        )
        client = LibationGameClient(this, config)
        decisionService = LibationDecisionService(config)
        bankingExecutor = BankingExecutor(client, config)
        blessingExecutor = BlessingExecutor(client)
        prayerRechargeExecutor = PrayerRechargeExecutor(client)
        sacrificeExecutor = SacrificeExecutor(client)

        ScriptLogging.info(logger, "Starting Libation Prayer")
        ScriptLogging.info(logger, "Wine type: ${config.wineType}")
        ScriptLogging.info(logger, "Minimum bone shards: ${config.minimumBoneShards}")

        addPaint(
            PaintBuilder.newBuilder()
                .x(40)
                .y(80)
                .addString("Task") { runtime.currentTask }
                .addString("Wine Type") { config.wineType }
                .addString("Bone shards") { client.boneShardCount().toString() }
                .addString("Blessed jugs") { client.blessedJugCount().toString() }
                .addString("Unblessed jugs") { client.unblessedJugCount().toString() }
                .trackSkill(Skill.Prayer)
                .build()
        )
    }

    override fun poll() {
        val state = client.stateSnapshot(runtime.needsInitialFullRecharge, runtime.awaitingBowlFill)
        when (decisionService.nextAction(state)) {
            LibationAction.BANK -> bankingExecutor.execute(runtime)
            LibationAction.BLESS -> blessingExecutor.execute(runtime)
            LibationAction.RECHARGE -> prayerRechargeExecutor.execute(runtime)
            LibationAction.SACRIFICE -> sacrificeExecutor.execute(runtime)
            LibationAction.IDLE -> runtime.currentTask = "Idle"
        }
        Condition.sleep(120)
    }

    override fun canBreak(): Boolean {
        return client.isNear(LibationConstants.BANK_TILE, 8)
    }
}


fun main() {
    val script = LibationPrayer()
    script.startScript("localhost", "0m6", false)
}

