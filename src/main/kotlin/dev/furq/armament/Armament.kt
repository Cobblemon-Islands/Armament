package dev.furq.armament

import dev.furq.armament.commands.ArmamentCommand
import dev.furq.armament.listeners.ArmorTrimListener
import dev.furq.armament.listeners.GUIListener
import dev.furq.armament.listeners.InventoryUpdateListener
import dev.furq.armament.utils.DatapackGenerator
import dev.furq.armament.utils.ResourcePackGenerator
import dev.furq.armament.utils.TabCompleter
import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class Armament : JavaPlugin() {
    private lateinit var messagesConfig: YamlConfiguration
    private lateinit var armorsConfig: YamlConfiguration

    override fun onEnable() {
        saveDefaultConfig()
        reloadConfig()
        loadConfigs()
        copyInitialFiles()

        server.pluginManager.apply {
            registerEvents(InventoryUpdateListener(this@Armament), this@Armament)
            registerEvents(ArmorTrimListener(this@Armament), this@Armament)
            registerEvents(GUIListener(this@Armament), this@Armament)
        }

        listOf("source_files", "resource_pack").forEach {
            File(dataFolder, it).mkdirs()
        }

        DatapackGenerator(this).generateDatapack()
        ResourcePackGenerator(this).generateResourcePack(
            File(dataFolder, "source_files"),
            File(dataFolder, "resource_pack")
        )

        getCommand("armament")?.apply {
            setExecutor(ArmamentCommand(this@Armament))
            tabCompleter = TabCompleter(this@Armament)
        }
    }

    override fun onDisable() {
        loadConfigs()
        DatapackGenerator(this).generateDatapack()
        logger.info("Armament plugin has been disabled!")

    }

    fun getMessage(key: String): String = ChatColor.translateAlternateColorCodes('&', messagesConfig.getString(key, "Message not found")!!)
    fun getArmorsConfig(): YamlConfiguration = armorsConfig

    fun loadConfigs() {
        armorsConfig = loadConfig("armors.yml")
        messagesConfig = loadConfig("messages.yml")
    }

    private fun loadConfig(fileName: String): YamlConfiguration {
        val file = File(dataFolder, fileName)
        if (!file.exists()) saveResource(fileName, false)
        return YamlConfiguration.loadConfiguration(file)
    }

    private fun copyInitialFiles() {
        val sourceFolder = File(dataFolder, "source_files")
        val itemFilesFolder = File(sourceFolder, "item_files")
        val layerFilesFolder = File(sourceFolder, "layer_files")

        itemFilesFolder.mkdirs()
        layerFilesFolder.mkdirs()

        if (config.getBoolean("generate_default_textures", true)) {
            val itemFiles = listOf(
                "epic_helmet.png",
                "epic_chestplate.png",
                "epic_leggings.png",
                "epic_boots.png"
            )
            val layerFiles = listOf(
                "epic_layer_1.png",
                "epic_layer_2.png"
            )
            copyResourceFiles("source_files/item_files", itemFiles, itemFilesFolder)
            copyResourceFiles("source_files/layer_files", layerFiles, layerFilesFolder)
        }
    }

    private fun copyResourceFiles(resourcePath: String, fileNames: List<String>, targetDir: File) {
        fileNames.forEach { fileName ->
            getResource("$resourcePath/$fileName")?.let { inputStream ->
                val targetFile = File(targetDir, fileName)
                if (!targetFile.exists()) {
                    targetFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }
    }
}