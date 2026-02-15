package org.powbot.community.ectofunctus.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.community.ectofunctus.EctofunctusConstants.BUCKET_OF_SLIME
import org.powbot.community.ectofunctus.EctofunctusConstants.DRAGON_BONEMEAL
import org.powbot.community.mixology.structure.TreeTask

class ShouldWorship : TreeTask(false) {
    override fun validate(): Boolean {
        val bonemealReady = Inventory.stream().name(DRAGON_BONEMEAL).isNotEmpty()
        val slimeReady = Inventory.stream().name(BUCKET_OF_SLIME).isNotEmpty()
        return bonemealReady && slimeReady
    }

    override fun toString(): String = "Checking if we can worship"
}

