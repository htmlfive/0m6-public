package org.powbot.community.ectofunctus.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.community.ectofunctus.EctofunctusConstants.BUCKET_OF_SLIME
import org.powbot.community.ectofunctus.EctofunctusConstants.DRAGON_BONEMEAL

class ShouldWorship {
    fun validate(): Boolean {
        val bonemealReady = Inventory.stream().name(DRAGON_BONEMEAL).isNotEmpty()
        val slimeReady = Inventory.stream().name(BUCKET_OF_SLIME).isNotEmpty()
        return bonemealReady && slimeReady
    }
}

