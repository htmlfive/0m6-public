package org.powbot.community.ectofunctus.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.community.ectofunctus.EctofunctusConstants.DRAGON_BONES

class ShouldProcessBones {
    fun validate(): Boolean {
        return Inventory.stream().name(DRAGON_BONES).isNotEmpty()
    }
}

