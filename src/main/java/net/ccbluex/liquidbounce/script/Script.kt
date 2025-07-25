/*
 * RinBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/rattermc/rinbounce69
 */
package net.ccbluex.liquidbounce.script

import jdk.internal.dynalink.beans.StaticClass
import jdk.nashorn.api.scripting.JSObject
import jdk.nashorn.api.scripting.NashornScriptEngineFactory
import jdk.nashorn.api.scripting.ScriptUtils
import net.ccbluex.liquidbounce.LiquidBounce.commandManager
import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.LiquidBounce.scriptManager
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.script.ScriptManager.scriptsFolder
import net.ccbluex.liquidbounce.script.api.ScriptCommand
import net.ccbluex.liquidbounce.script.api.ScriptModule
import net.ccbluex.liquidbounce.script.api.ScriptTab
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.script.api.global.Item
import net.ccbluex.liquidbounce.script.api.global.Setting
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import java.io.File
import java.util.function.Function
import javax.script.ScriptEngine

class Script(val scriptFile: File) : MinecraftInstance {

    private val scriptEngine: ScriptEngine
    private val scriptText = scriptFile.readText()

    // Script information
    lateinit var scriptName: String
    lateinit var scriptVersion: String
    lateinit var scriptAuthors: Array<String>

    private var state = false

    private val events = mutableMapOf<String, JSObject>()

    private val registeredModules = mutableListOf<Module>()
    private val registeredCommands = mutableListOf<Command>()

    init {
        val engineFlags = getMagicComment("engine_flags")?.split(',')?.toTypedArray() ?: emptyArray()
        scriptEngine = NashornScriptEngineFactory().getScriptEngine(*engineFlags)

        // Global classes
        scriptEngine.run {
            put("Chat", StaticClass.forClass(Chat::class.java))
            put("Setting", StaticClass.forClass(Setting::class.java))
            put("Item", StaticClass.forClass(Item::class.java))

            // Global instances
            put("mc", mc)

            put("moduleManager", moduleManager)
            put("commandManager", commandManager)
            put("scriptManager", scriptManager)

            // Global functions
            put("registerScript", RegisterScript())
        }
    }

    fun initScript() {
        scriptEngine.eval(scriptText)

        callEvent("load")

        LOGGER.info("[ScriptAPI] Successfully loaded script '${scriptFile.name}'.")
    }

    @Suppress("UNCHECKED_CAST")
    inner class RegisterScript : Function<JSObject, Script> {
        /**
         * Global function 'registerScript' which is called to register a script.
         * @param scriptObject JavaScript object containing information about the script.
         * @return The instance of this script.
         */
        override fun apply(scriptObject: JSObject): Script {
            scriptName = scriptObject.getMember("name") as String
            scriptVersion = scriptObject.getMember("version") as String
            scriptAuthors = ScriptUtils.convert(scriptObject.getMember("authors"), Array<String>::class.java) as Array<String>

            return this@Script
        }
    }

    /**
     * Registers a new script module.
     * @param moduleObject JavaScript object containing information about the module.
     * @param callback JavaScript function to which the corresponding instance of [ScriptModule] is passed.
     * @see ScriptModule
     */
    @Suppress("unused")
    fun registerModule(moduleObject: JSObject, callback: JSObject) {
        val name = moduleObject.getMember("name") as String
        val description = moduleObject.getMember("description") as String
        val categoryString = moduleObject.getMember("category") as String
        val category = Category.entries.find {
            it.displayName.equals(categoryString, true)
        } ?: Category.FUN


        val module = ScriptModule(name, category, description, moduleObject)
        moduleManager.registerModule(module)
        registeredModules += module
        callback.call(moduleObject, module)
    }

    /**
     * Registers a new script command.
     * @param commandObject JavaScript object containing information about the command.
     * @param callback JavaScript function to which the corresponding instance of [ScriptCommand] is passed.
     * @see ScriptCommand
     */
    @Suppress("unused")
    fun registerCommand(commandObject: JSObject, callback: JSObject) {
        val command = ScriptCommand(commandObject)
        commandManager.registerCommand(command)
        registeredCommands += command
        callback.call(commandObject, command)
    }

    /**
     * Registers a new creative inventory tab.
     * @param tabObject JavaScript object containing information about the tab.
     * @see ScriptTab
     */
    @Suppress("unused")
    fun registerTab(tabObject: JSObject) {
        ScriptTab(tabObject)
    }

    /**
     * Gets the value of a magic comment from the script. Used for specifying additional information about the script.
     * @param name Name of the comment.
     * @return Value of the comment.
     */
    private fun getMagicComment(name: String): String? {
        val magicPrefix = "///"

        scriptText.lineSequence().forEach {
            if (!it.startsWith(magicPrefix)) return null

            val commentData = it.subSequence(magicPrefix.length, it.length).split("=", limit = 2)

            if (commentData.first().trim() == name) {
                return commentData.last().trim()
            }
        }

        return null
    }

    /**
     * Called from inside the script to register a new event handler.
     * @param eventName Name of the event.
     * @param handler JavaScript function used to handle the event.
     */
    fun on(eventName: String, handler: JSObject) {
        events[eventName] = handler
    }

    /**
     * Called when the client enables the script.
     */
    fun onEnable() {
        if (state) return

        callEvent("enable")
        state = true
    }

    /**
     * Called when the client disables the script. Handles unregistering all modules and commands
     * created with this script.
     */
    fun onDisable() {
        if (!state) return

        registeredModules.forEach { moduleManager.unregisterModule(it) }
        registeredCommands.forEach { commandManager.unregisterCommand(it) }

        callEvent("disable")
        state = false
    }

    /**
     * Imports another JavaScript file into the context of this script.
     * @param scriptFile Path to the file to be imported.
     */
    fun import(scriptFile: String) {
        val scriptText = File(scriptsFolder, scriptFile).readText()

        scriptEngine.eval(scriptText)
    }

    /**
     * Calls the handler of a registered event.
     * @param eventName Name of the event to be called.
     */
    private fun callEvent(eventName: String) {
        try {
            events[eventName]?.call(null)
        } catch (throwable: Throwable) {
            LOGGER.error("[ScriptAPI] Exception in script '$scriptName'!", throwable)
        }
    }
}