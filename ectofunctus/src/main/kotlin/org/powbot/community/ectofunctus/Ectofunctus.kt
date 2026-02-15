package org.powbot.community.ectofunctus

import org.powbot.api.rt4.Skills
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.ScriptCategory
import org.powbot.api.script.ScriptManifest
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.community.ectofunctus.tasks.BankTask
import org.powbot.community.ectofunctus.tasks.IdleTask
import org.powbot.community.ectofunctus.tasks.ProcessBonesTask
import org.powbot.community.ectofunctus.tasks.ShouldBank
import org.powbot.community.ectofunctus.tasks.ShouldProcessBones
import org.powbot.community.ectofunctus.tasks.ShouldWorship
import org.powbot.community.ectofunctus.tasks.WorshipTask
import org.powbot.community.mixology.structure.TreeScript

@ScriptManifest(
    name = "Ectofunctus",
    description = "Grinds dragon bones, worships the Ectofuntus, and restocks from Castle Wars automatically.",
    version = "1.0.0",
    author = "0m6",
    category = ScriptCategory.Prayer
)
class Ectofunctus : TreeScript() {
    private var startPrayerXp: Int = 0

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val script = Ectofunctus()
            script.startScript("localhost", "0m6", false)
        }
    }

    override fun onStart() {
        startPrayerXp = Skills.experience(Skill.Prayer)
        addNotedPosition("castle_wars_bank", EctofunctusConstants.CASTLE_WARS_BANK_TILE)
        addNotedPosition("grinder", EctofunctusConstants.GRINDER_TILE)
        addNotedPosition("ectofuntus", EctofunctusConstants.ECTOFUNTUS_TILE)
        setNotedValue("batch_size", EctofunctusConstants.DEFAULT_BATCH_SIZE)

        val head = ShouldBank()
        val processDecision = head.setLeft(ShouldProcessBones())
        val worshipDecision = processDecision.setLeft(ShouldWorship())
        worshipDecision.setLeft(IdleTask())

        head.setRight(BankTask(this))
        processDecision.setRight(ProcessBonesTask(this))
        worshipDecision.setRight(WorshipTask(this))

        addPaint(
            PaintBuilder.newBuilder()
                .trackSkill(Skill.Prayer)
                .addString("Status") { getTaskDescription() ?: "Initializing" }
                .build()
        )

        setHead(head)
    }

    override fun poll() {
        if (!hasHeadBeenSet()) return
        traverseTree()
        super.poll()
    }
}

