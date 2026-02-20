package org.powbot.community.maplefletcher

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.AbstractScript
import org.powbot.api.script.OptionType
import org.powbot.api.script.ScriptCategory
import org.powbot.api.script.ScriptConfiguration
import org.powbot.api.script.ScriptManifest
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.community.maplefletcher.tasks.BankBows
import org.powbot.community.maplefletcher.tasks.ChopMaples
import org.powbot.community.maplefletcher.tasks.FletchMaples
import org.powbot.community.maplefletcher.tasks.NeedsKnife
import org.powbot.community.maplefletcher.tasks.ShouldBank
import org.powbot.community.maplefletcher.tasks.ShouldFletch
import org.powbot.community.maplefletcher.tasks.Task
import org.powbot.community.maplefletcher.tasks.WithdrawKnife

@ScriptManifest(
    name = "0m6 Maple Fletcher",
    description = "Chops maple logs, fletches configurable maple products, and banks them for endless training.",
    version = "1.0.0",
    author = "0m6",
    scriptId = "750e7c78-d811-45f3-a2c7-0f23417a9804",
    category = ScriptCategory.Woodcutting
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            name = "Fletch To",
            description = "Item to make from maple logs.",
            optionType = OptionType.STRING,
            defaultValue = "Maple longbow (u)",
            allowedValues = [
                "Arrow shaft",
                "Maple shortbow (u)",
                "Maple longbow (u)",
                "Maple shield"
            ]
        )
    ]
)
class MapleFletcher : AbstractScript() {
    val treeTile: Tile = MapleFletcherConstants.TREE_TILE
    val bankTile: Tile = MapleFletcherConstants.BANK_TILE
    var logsCut: Int = 0
    var bowsMade: Int = 0
    var bowsBanked: Int = 0
    var configuredFletchTo: MapleFletcherConstants.FletchTo = MapleFletcherConstants.FletchTo.MAPLE_LONGBOW_U
    private var taskDescription: String = "Initializing"
    private var lastSeenMapleLogCount: Int = -1
    private var lastSeenMadeItemCount: Int = -1

    private lateinit var shouldBank: ShouldBank
    private lateinit var shouldFletch: ShouldFletch
    private lateinit var needsKnife: NeedsKnife
    private lateinit var bankBows: BankBows
    private lateinit var fletchMaples: FletchMaples
    private lateinit var withdrawKnife: WithdrawKnife
    private lateinit var chopMaples: ChopMaples

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MapleFletcher().startScript("localhost", "0m6", false)
        }
    }

    override fun onStart() {
        logsCut = 0
        bowsMade = 0
        bowsBanked = 0
        configuredFletchTo = MapleFletcherConstants.FletchTo.fromLabel(getOption<String>("Fletch To"))
        lastSeenMapleLogCount = currentMapleLogCount()
        lastSeenMadeItemCount = currentMadeItemCount()
        shouldBank = ShouldBank()
        shouldFletch = ShouldFletch()
        needsKnife = NeedsKnife()
        bankBows = BankBows(this)
        fletchMaples = FletchMaples(this)
        withdrawKnife = WithdrawKnife(this)
        chopMaples = ChopMaples(this)

        addPaint(
            PaintBuilder.newBuilder()
                .x(20)
                .y(70)
                .trackSkill(Skill.Woodcutting)
                .trackSkill(Skill.Fletching)
                .addString("Task:") { taskDescription }
                .addString("Fletch To:") { configuredFletchTo.label }
                .addString("Logs Cut:") { logsCut.toString() }
                .addString("Items Made:") { bowsMade.toString() }
                .addString("Items Banked:") { bowsBanked.toString() }
                .build()
        )
    }

    override fun poll() {
        updateInventoryCounters()

        val selectedTask: Task = when {
            shouldBank.validate() -> bankBows
            shouldFletch.validate() && needsKnife.validate() -> withdrawKnife
            shouldFletch.validate() -> fletchMaples
            else -> chopMaples
        }
        taskDescription = selectedTask.toString()
        Condition.sleep(selectedTask.execute())
    }

    private fun updateInventoryCounters() {
        val mapleLogsNow = currentMapleLogCount()
        if (lastSeenMapleLogCount >= 0 && mapleLogsNow > lastSeenMapleLogCount) {
            logsCut += (mapleLogsNow - lastSeenMapleLogCount)
        }
        lastSeenMapleLogCount = mapleLogsNow

        val madeItemsNow = currentMadeItemCount()
        if (lastSeenMadeItemCount >= 0 && madeItemsNow > lastSeenMadeItemCount) {
            bowsMade += (madeItemsNow - lastSeenMadeItemCount)
        }
        lastSeenMadeItemCount = madeItemsNow
    }

    private fun currentMapleLogCount(): Int {
        return Inventory.stream().name(MapleFletcherConstants.MAPLE_LOG_NAME).count(true).toInt()
    }

    private fun currentMadeItemCount(): Int {
        return Inventory.stream().name(configuredFletchTo.productName).count(true).toInt()
    }
}

fun main() {
    MapleFletcher().startScript("localhost", "0m6", false)
}
