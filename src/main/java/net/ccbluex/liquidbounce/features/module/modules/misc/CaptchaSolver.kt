package net.ccbluex.liquidbounce.features.module.modules.misc

import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.ccbluex.liquidbounce.utils.io.post
import net.minecraft.item.ItemMap
import net.minecraft.item.ItemStack
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject

class CaptchaSolver : Module("CaptchaSolver", Category.MISC) {
    private var lastMapData: String? = null
    private var captchaJob: Job? = null
    private val apiEndpoint = "https://60f254355a67.ngrok-free.app/ocr"
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onEnable() {
        captchaJob = coroutineScope.launch {
            while (isActive) {
                withContext(Dispatchers.Main) {
                    solveCaptchaIfNeeded()
                }
                delay(1000)
            }
        }
    }

    override fun onDisable() {
        captchaJob?.cancel()
        captchaJob = null
    }

    private fun solveCaptchaIfNeeded() {
        val player = MinecraftInstance.mc.thePlayer ?: return
        val stack: ItemStack? = player.inventory.mainInventory.getOrNull(0)
        if (stack?.item !is ItemMap) return

        // Get the map ID from the item and retrieve the MapData
        val mapId = stack.metadata // or stack.itemDamage depending on mapping
        
        val mapData = ItemMap.loadMapData(mapId, MinecraftInstance.mc.theWorld) ?: return

        // Get the raw color buffer (128x128 = 16384 bytes)
        val colorBuffer: ByteArray = mapData.colors

        // Convert byte array to unsigned int list for JSON
        val colorList = colorBuffer.map { it.toInt() and 0xFF }

        // Check for duplication to avoid resending same map
        val currentMapHash = colorList.joinToString(",")
        if (currentMapHash == lastMapData) {
            println("[CaptchaSolver] Debug: map data unchanged, skipping.")
            return
        }

        lastMapData = currentMapHash
        println("[CaptchaSolver] Debug: sending map buffer to OCR...")

        // Launch background coroutine to send to OCR API
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("image", JSONArray(colorList))
                }

                val body = RequestBody.create(
                    "application/json".toMediaTypeOrNull(),
                    json.toString()
                )

                val response = HttpClient.post(apiEndpoint, body)
                val responseBody = response.body?.string()

                val result = JSONObject(responseBody).optString("result", "")
                if (result.isNotBlank()) {
                    println("[CaptchaSolver] Debug: OCR result = $result")
                    withContext(Dispatchers.Main) {
                        MinecraftInstance.mc.thePlayer.sendChatMessage(result)
                    }
                } else {
                    println("[CaptchaSolver] Debug: OCR returned empty result.")
                }
            } catch (e: Exception) {
                println("[CaptchaSolver] Error: ${e.message}")
            }
        }
    }
}
