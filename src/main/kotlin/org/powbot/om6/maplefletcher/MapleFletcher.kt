package org.powbot.om6.maplefletcher

import org.powbot.api.script.ScriptCategory
import org.powbot.api.script.ScriptManifest
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.om6.maplefletcher.tasks.BankBows
import org.powbot.om6.maplefletcher.tasks.ChopMaples
import org.powbot.om6.maplefletcher.tasks.FletchMaples
import org.powbot.om6.maplefletcher.tasks.NeedsKnife
import org.powbot.om6.maplefletcher.tasks.ShouldBank
import org.powbot.om6.maplefletcher.tasks.ShouldFletch
import org.powbot.om6.maplefletcher.tasks.WithdrawKnife
import org.powbot.om6.mixology.structure.TreeScript

@ScriptManifest(
    name = "0m6 Maple Fletcher",
    description = "Chops maple logs, fletches Maple longbow (u), and banks them for endless training.",
    version = "1.0.0",
    author = "0m6",
    category = ScriptCategory.Woodcutting
)
class MapleFletcher : TreeScript() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MapleFletcher().startScript("localhost", "0m6", false)
        }
    }

    override fun onStart() {
        wipeCachedData()
        addNotedPosition("maple_tree_tile", MapleFletcherConstants.TREE_TILE)
        addNotedPosition("maple_bank_tile", MapleFletcherConstants.BANK_TILE)
        setNotedValue("logs_cut", 0)
        setNotedValue("bows_made", 0)
        setNotedValue("bows_banked", 0)

        addPaint(
            PaintBuilder.newBuilder()
                .x(20)
                .y(70)
                .trackSkill(Skill.Woodcutting)
                .trackSkill(Skill.Fletching)
                .addString("Task:") { getTaskDescription() ?: "" }
                .addString("Logs Cut:") { getNotedValue("logs_cut").toString() }
                .addString("Bows Made:") { getNotedValue("bows_made").toString() }
                .addString("Bows Banked:") { getNotedValue("bows_banked").toString() }
                .build()
        )

        val head = ShouldBank()
        val fletchNode = head.setLeft(ShouldFletch())
        val needsKnifeNode = fletchNode.setLeft(NeedsKnife())
        head.setRight(BankBows(this))
        fletchNode.setRight(FletchMaples(this))
        needsKnifeNode.setRight(WithdrawKnife(this))
        needsKnifeNode.setLeft(ChopMaples(this))
        setHead(head)
    }

    override fun poll() {
        if (!hasHeadBeenSet()) return
        traverseTree()
        super.poll()
    }
}
