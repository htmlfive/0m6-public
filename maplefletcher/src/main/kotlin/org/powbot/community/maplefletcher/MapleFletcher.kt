package org.powbot.community.maplefletcher

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.AbstractScript
import org.powbot.api.script.ScriptCategory
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
    description = "Chops maple logs, fletches Maple longbow (u), and banks them for endless training.",
    version = "1.0.0",
    author = "0m6",
    category = ScriptCategory.Woodcutting
)
class MapleFletcher : AbstractScript() {
    val treeTile: Tile = MapleFletcherConstants.TREE_TILE
    val bankTile: Tile = MapleFletcherConstants.BANK_TILE
    var logsCut: Int = 0
    var bowsMade: Int = 0
    var bowsBanked: Int = 0
    private var taskDescription: String = "Initializing"

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
                .addString("Logs Cut:") { logsCut.toString() }
                .addString("Bows Made:") { bowsMade.toString() }
                .addString("Bows Banked:") { bowsBanked.toString() }
                .build()
        )
    }

    override fun poll() {
        val selectedTask: Task = when {
            shouldBank.validate() -> bankBows
            shouldFletch.validate() && needsKnife.validate() -> withdrawKnife
            shouldFletch.validate() -> fletchMaples
            else -> chopMaples
        }
        taskDescription = selectedTask.toString()
        Condition.sleep(selectedTask.execute())
    }
}

fun main() {
    MapleFletcher().startScript("localhost", "0m6", false)
}
