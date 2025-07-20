package net.ccbluex.liquidbounce.features.module.modules.misc

import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.ccbluex.liquidbounce.utils.io.post
import net.minecraft.item.ItemMap
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
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

        val mapData: ByteArray? = stack.tagCompound?.getByteArray("data")
        if (mapData != null) {
            val dataString = mapData.joinToString(",")
            if (dataString == lastMapData) return
            lastMapData = dataString

            println("[CaptchaSolver] Sending image data to OCR...")

            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val json = JSONObject().apply { put("image", dataString) }
                    val body = RequestBody.create(
                        "application/json".toMediaTypeOrNull(),
                        json.toString()
                    )
                    val response = HttpClient.post(apiEndpoint, body)
                    val result = JSONObject(response.body?.string()).optString("result", "")

                    if (result.isNotBlank()) {
                        withContext(Dispatchers.Main) {
                            MinecraftInstance.mc.thePlayer.sendChatMessage(result)
                        }
                    } else {
                        println("[CaptchaSolver] No result returned from OCR.")
                    }
                } catch (e: Exception) {
                    println("[CaptchaSolver] Error during OCR request: ${e.message}")
                }
            }
        }
    }
}
