/*
 * RinBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/rattermc/rinbounce69
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.startY
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextDouble
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe

object WatchCat : FlyMode("WatchCat") {
    override fun onUpdate() {
        strafe(0.15f)
        mc.thePlayer.isSprinting = true

        if (mc.thePlayer.posY < startY + 2) {
            mc.thePlayer.motionY = nextDouble(endInclusive = 0.5)
            return
        }

        if (startY > mc.thePlayer.posY) mc.thePlayer.stopXZ()
    }
}
