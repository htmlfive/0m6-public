package org.powbot.community.communitypowersalvage.config

import org.powbot.api.rt4.Sailing
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

fun sailingAngle(): Int = Sailing.angle().let { if (it.valid()) it.angle else 0 }

object Constants {
    val ACTIVE_SHIPWRECK_IDS = intArrayOf(
        60464, 60466, 60468, 60470, 60472, 60474, 60476, 60478,
    )

    data class AngleEntry(
        val hX1: Int, val hX2: Int, val hY1: Int, val hY2: Int, val hZ1: Int, val hZ2: Int,
        val sX1: Int, val sX2: Int, val sY1: Int, val sY2: Int, val sZ1: Int, val sZ2: Int,
        val cX1: Int, val cX2: Int, val cY1: Int, val cY2: Int, val cZ1: Int, val cZ2: Int,
    ) {
        fun rotated(angle: Int): AngleEntry {
            val rad = angle * 2.0 * PI / 2048.0
            val cos = cos(rad)
            val sin = sin(rad)
            fun box(x1: Int, x2: Int, y1: Int, y2: Int, z1: Int, z2: Int): SixInts {
                val bx1 = min(x1, x2); val bx2 = max(x1, x2)
                val by1 = min(y1, y2); val by2 = max(y1, y2)
                val bz1 = min(z1, z2); val bz2 = max(z1, z2)
                val cornersX = doubleArrayOf(bx1 * cos - by1 * sin, bx1 * cos - by2 * sin, bx2 * cos - by1 * sin, bx2 * cos - by2 * sin)
                val cornersY = doubleArrayOf(bx1 * sin + by1 * cos, bx1 * sin + by2 * cos, bx2 * sin + by1 * cos, bx2 * sin + by2 * cos)
                return SixInts(cornersX.min().roundToInt(), cornersX.max().roundToInt(), cornersY.min().roundToInt(), cornersY.max().roundToInt(), bz1, bz2)
            }
            val h = box(hX1, hX2, hY1, hY2, hZ1, hZ2)
            val s = box(sX1, sX2, sY1, sY2, sZ1, sZ2)
            val c = box(cX1, cX2, cY1, cY2, cZ1, cZ2)
            return AngleEntry(h.x1, h.x2, h.y1, h.y2, h.z1, h.z2, s.x1, s.x2, s.y1, s.y2, s.z1, s.z2, c.x1, c.x2, c.y1, c.y2, c.z1, c.z2)
        }
        private data class SixInts(val x1: Int, val x2: Int, val y1: Int, val y2: Int, val z1: Int, val z2: Int)
    }

    object Bounds {
        fun forAngle(angle: Int): AngleEntry = BY_ANGLE[angle] ?: BASE_ENTRY.rotated(angle)

        private val BASE_ENTRY = AngleEntry(378, 292, -144, -140, -32, 32, -312, -278, -94, -70, -62, 32, 248, 172, -114, -40, 8, 62)

        val BY_ANGLE: Map<Int, AngleEntry> = mapOf(
            0 to AngleEntry(378, 292, -144, -140, -32, 32, -312, -278, -94, -70, -62, 32, 248, 172, -114, -40, 8, 62),
            128 to AngleEntry(258, 361, -144, -140, -174, -82, -312, -245, -94, -70, 49, 149, 162, 253, -114, -40, -88, -9),
            256 to AngleEntry(184, 290, -144, -140, -290, -184, -264, -174, -94, -70, 153, 243, 127, 219, -114, -40, -170, -78),
            384 to AngleEntry(82, 174, -144, -140, -361, -258, -177, -77, -94, -70, 233, 300, 73, 152, -114, -40, -226, -135),
            512 to AngleEntry(-32, 32, -144, -140, -378, -292, -62, 32, -94, -70, 278, 312, 8, 62, -114, -40, -248, -172),
            640 to AngleEntry(-174, -82, -144, -140, -361, -258, 49, 149, -94, -70, 245, 312, -88, -9, -114, -40, -253, -162),
            768 to AngleEntry(-290, -184, -144, -140, -290, -184, 153, 243, -94, -70, 174, 264, -170, -78, -114, -40, -219, -127),
            896 to AngleEntry(-361, -258, -144, -140, -174, -82, 233, 300, -94, -70, 77, 177, -226, -135, -114, -40, -152, -73),
            1024 to AngleEntry(268, 182, -144, -140, 32, -32, 278, 312, -94, -70, -32, 62, -248, -172, -114, -40, -62, 8),
            1152 to AngleEntry(-361, -258, -144, -140, 82, 174, 245, 312, -94, -70, -149, -49, -253, -162, -114, -40, 9, 88),
            1280 to AngleEntry(-290, -184, -144, -140, 184, 290, 174, 264, -94, -70, -243, -153, -219, -127, -114, -40, 78, 170),
            1408 to AngleEntry(-174, -82, -144, -140, 258, 361, 77, 177, -94, -70, -300, -233, -152, -73, -114, -40, 135, 226),
            1536 to AngleEntry(-32, 32, -144, -140, 292, 378, -32, 62, -94, -70, -312, -278, -62, -8, -114, -40, 172, 248),
            1664 to AngleEntry(82, 174, -144, -140, 258, 361, -149, -49, -94, -70, -312, -245, 9, 88, -114, -40, 162, 253),
            1792 to AngleEntry(184, 290, -144, -140, 184, 290, -243, -153, -94, -70, -264, -174, 78, 170, -114, -40, 127, 219),
            1920 to AngleEntry(258, 361, -144, -140, 82, 174, -300, -233, -94, -70, -177, -77, 135, 226, -114, -40, 73, 152),
        )
    }
}
