/*
 * RinBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/rattermc/rinbounce69
 */
package net.ccbluex.liquidbounce.utils.movement

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import kotlin.math.sqrt

object BPSUtils : MinecraftInstance, Listenable {

    private var lastPosX: Double = 0.0
    private var lastPosZ: Double = 0.0
    private var lastTimestamp: Long = 0

    fun getBPS(): Double {
        val player = mc.thePlayer ?: return 0.0

        if (player.ticksExisted < 1 || mc.theWorld == null) {
            return 0.0
        }

        val currentTime = System.currentTimeMillis()
        val deltaTime = currentTime - lastTimestamp
        val deltaX = player.posX - lastPosX
        val deltaZ = player.posZ - lastPosZ
        val distance = sqrt(deltaX * deltaX + deltaZ * deltaZ)

        if (deltaTime <= 0 || distance <= 0) {
            return 0.0
        }

        val bps = distance * (1000 / deltaTime.toDouble())

        lastPosX = player.posX
        lastPosZ = player.posZ
        lastTimestamp = currentTime

        return bps
    }

    
}