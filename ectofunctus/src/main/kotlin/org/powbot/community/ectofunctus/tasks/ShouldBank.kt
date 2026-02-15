package org.powbot.community.ectofunctus.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.community.ectofunctus.EctofunctusConstants.BUCKET_OF_SLIME
import org.powbot.community.ectofunctus.EctofunctusConstants.DRAGON_BONES
import org.powbot.community.ectofunctus.EctofunctusConstants.DRAGON_BONEMEAL

class ShouldBank {
    fun validate(): Boolean {
        val bones = Inventory.stream().name(DRAGON_BONES).count().toInt()
        val bonemeal = Inventory.stream().name(DRAGON_BONEMEAL).count().toInt()
        val slime = Inventory.stream().name(BUCKET_OF_SLIME).count().toInt()
        val noSupplies = bones == 0 && bonemeal == 0
        val missingSlime = bonemeal > 0 && slime == 0
        val noSlimeForBones = bones > 0 && slime == 0

        return noSupplies || missingSlime || noSlimeForBones
    }
}

