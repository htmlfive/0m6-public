package org.powbot.community.ectofunctus.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.community.ectofunctus.EctofunctusConstants.DRAGON_BONES
import org.powbot.community.mixology.structure.TreeTask

class ShouldProcessBones : TreeTask(false) {
    override fun validate(): Boolean {
        return Inventory.stream().name(DRAGON_BONES).isNotEmpty()
    }

    override fun toString(): String = "Deciding whether to grind bones"
}

