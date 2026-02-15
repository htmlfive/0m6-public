package org.powbot.om6.ectofunctus.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.om6.ectofunctus.EctofunctusConstants.DRAGON_BONES
import org.powbot.om6.mixology.structure.TreeTask

class ShouldProcessBones : TreeTask(false) {
    override fun validate(): Boolean {
        return Inventory.stream().name(DRAGON_BONES).isNotEmpty()
    }

    override fun toString(): String = "Deciding whether to grind bones"
}
