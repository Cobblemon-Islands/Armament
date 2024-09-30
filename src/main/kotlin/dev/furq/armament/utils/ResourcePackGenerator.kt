package dev.furq.armament.utils

import com.google.gson.Gson
import dev.furq.armament.Armament
import org.bukkit.configuration.file.FileConfiguration
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ResourcePackGenerator(private val plugin: Armament) {
    private val materialGetter = MaterialGetter(plugin)
    private val gson = Gson()

    fun generateResourcePack(sourceFolder: File, targetFolder: File) {
        val armorsConfig = plugin.getArmorsConfig()
        listOf("item_files", "layer_files").forEach { File(sourceFolder, it).mkdirs() }

        cleanupResourcePack(targetFolder, armorsConfig)
        validateResourcePackConfiguration(armorsConfig, sourceFolder)

        val directories = listOf(
            "assets/minecraft/models/item",
            "assets/minecraft/models/armors",
            "assets/minecraft/textures/item/armors",
            "assets/armament/textures/trims/models/armor",
            "assets/minecraft/atlases",
            "assets/minecraft/textures/models/armor"
        )
        directories.forEach { File(targetFolder, it).mkdirs() }
        copyResourceFolder(
            "resource_pack/assets/minecraft/textures/models/armor",
            File(targetFolder, "assets/minecraft/textures/models/armor")
        )

        val material = materialGetter.getArmorString().lowercase()
        armorsConfig.getConfigurationSection("armors")?.getKeys(false)?.forEach { armorName ->
            val armorSection = armorsConfig.getConfigurationSection("armors.$armorName")
            listOf("helmet", "chestplate", "leggings", "boots").forEach { piece ->
                if (armorSection?.contains(piece) == true) {
                    copyFile(
                        File(sourceFolder, "item_files/${armorName}_$piece.png"),
                        File(targetFolder, "assets/minecraft/textures/item/armors/${armorName}_$piece.png")
                    )

                    val modelData = mapOf(
                        "parent" to "item/generated",
                        "textures" to mapOf(
                            "layer0" to "item/armors/${armorName}_$piece",
                            "layer1" to "item/armors/${armorName}_$piece"
                        )
                    )
                    File(targetFolder, "assets/minecraft/models/armors/${armorName}_$piece.json").writeText(
                        gson.toJson(
                            modelData
                        )
                    )
                }
            }

            copyFile(
                File(sourceFolder, "layer_files/${armorName}_layer_1.png"),
                File(targetFolder, "assets/armament/textures/trims/models/armor/$armorName.png")
            )
            copyFile(
                File(sourceFolder, "layer_files/${armorName}_layer_2.png"),
                File(targetFolder, "assets/armament/textures/trims/models/armor/${armorName}_leggings.png")
            )
        }

        listOf("helmet", "chestplate", "leggings", "boots").forEach { piece ->
            val overrides = armorsConfig.getConfigurationSection("armors")?.getKeys(false)?.mapNotNull { armorName ->
                val customModelData = armorsConfig.getInt("armors.$armorName.custom_model_data")
                mapOf(
                    "predicate" to mapOf("custom_model_data" to customModelData),
                    "model" to "armors/${armorName}_$piece"
                )
            } ?: emptyList()

            val materialData = mapOf(
                "parent" to "item/generated",
                "textures" to mapOf(
                    "layer0" to "minecraft:item/${material}_$piece"
                ),
                "overrides" to overrides
            )
            File(targetFolder, "assets/minecraft/models/item/${material}_$piece.json").writeText(
                gson.toJson(
                    materialData
                )
            )
        }

        val armorTrimsData = generateArmorTrimsJson(armorsConfig)
        File(targetFolder, "assets/minecraft/atlases/armor_trims.json").writeText(gson.toJson(armorTrimsData))

        val blocksData = generateBlocksJson(armorsConfig)
        File(targetFolder, "assets/minecraft/atlases/blocks.json").writeText(gson.toJson(blocksData))

        File(targetFolder, "pack.mcmeta").writeText(
            """
            {
                "pack": {
                    "pack_format": 34,
                    "description": "Custom Armors Resource Pack by Armament"
                }
            }
        """.trimIndent()
        )

        zipResourcePack(targetFolder)
    }

    private fun generateArmorTrimsJson(armorsConfig: FileConfiguration): Map<String, Any> {
        val textures = armorsConfig.getConfigurationSection("armors")?.getKeys(false)?.flatMap { armorName ->
            listOf("armament:trims/models/armor/$armorName", "armament:trims/models/armor/${armorName}_leggings")
        } ?: emptyList()

        return mapOf(
            "sources" to listOf(
                mapOf(
                    "type" to "minecraft:paletted_permutations",
                    "palette_key" to "trims/color_palettes/trim_palette",
                    "permutations" to mapOf(
                        "quartz" to "trims/color_palettes/quartz",
                        "iron" to "trims/color_palettes/iron",
                        "gold" to "trims/color_palettes/gold",
                        "diamond" to "trims/color_palettes/diamond",
                        "netherite" to "trims/color_palettes/netherite",
                        "redstone" to "trims/color_palettes/redstone",
                        "copper" to "trims/color_palettes/copper",
                        "emerald" to "trims/color_palettes/emerald",
                        "lapis" to "trims/color_palettes/lapis",
                        "amethyst" to "trims/color_palettes/amethyst"
                    ),
                    "textures" to textures + listOf(
                        "minecraft:armor/diamond",
                        "minecraft:armor/diamond_leggings"
                    )
                )
            )
        )
    }

    private fun generateBlocksJson(armorsConfig: FileConfiguration): Map<String, Any> {
        val sources = armorsConfig.getConfigurationSection("armors")?.getKeys(false)?.flatMap { armorName ->
            listOf(
                mapOf(
                    "type" to "single",
                    "resource" to "armament:trims/models/armor/${armorName}_leggings",
                    "sprite" to "armament:trims/models/armor/${armorName}_leggings"
                ),
                mapOf(
                    "type" to "single",
                    "resource" to "armament:trims/models/armor/$armorName",
                    "sprite" to "armament:trims/models/armor/$armorName"
                )
            )
        } ?: emptyList()

        return mapOf("sources" to sources)
    }

    private fun copyFile(source: File, destination: File) {
        if (source.exists()) {
            source.copyTo(destination, overwrite = true)
        }
    }

    private fun copyResourceFolder(resourcePath: String, targetDir: File) {
        val material = materialGetter.getArmorString().lowercase()
        val resourceFiles = listOf(
            "transparent_layer.png" to "${material}_layer_1.png",
            "transparent_layer.png" to "${material}_layer_2.png",
        )

        resourceFiles.forEach { (sourceName, targetName) ->
            plugin.getResource("$resourcePath/$sourceName")?.let { inputStream ->
                val targetFile = File(targetDir, targetName)
                targetFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }

    private fun zipResourcePack(targetFolder: File) {
        val zipFile = File(targetFolder.parentFile, "${targetFolder.name}.zip")
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
            targetFolder.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entryName = targetFolder.toPath().relativize(file.toPath()).toString().replace("\\", "/")
                    zipOut.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { it.copyTo(zipOut) }
                    zipOut.closeEntry()
                }
            }
        }
    }

    private fun cleanupResourcePack(targetFolder: File, armorsConfig: FileConfiguration) {
        val currentArmors = armorsConfig.getConfigurationSection("armors")?.getKeys(false) ?: emptySet()
        val material = materialGetter.getArmorString().lowercase()

        val pngDirectories = listOf(
            "assets/minecraft/textures/item/armors",
            "assets/minecraft/models/armors",
            "assets/minecraft/textures/models/armor",
            "assets/armament/textures/trims/models/armor"

        )
        pngDirectories.forEach { dir ->
            File(targetFolder, dir).listFiles()?.forEach { file ->
                val armorName = file.nameWithoutExtension.substringBeforeLast("_")
                if (armorName !in currentArmors) {
                    file.delete()
                }
            }
        }
        File(targetFolder, "assets/minecraft/models/item").listFiles()?.forEach { file ->
            if (!file.name.startsWith(material)) {
                file.delete()
            }
        }
    }

    private fun validateResourcePackConfiguration(armorsConfig: FileConfiguration, sourceFolder: File) {
        val configArmors = armorsConfig.getConfigurationSection("armors")?.getKeys(false) ?: emptySet()

        val itemFiles =
            File(sourceFolder, "item_files").listFiles { file -> file.extension.equals("png", ignoreCase = true) }
                ?.map { it.nameWithoutExtension.substringBeforeLast("_") }
                ?.toSet() ?: emptySet()

        val layerFiles =
            File(sourceFolder, "layer_files").listFiles { file -> file.extension.equals("png", ignoreCase = true) }
                ?.map { it.nameWithoutExtension.substringBeforeLast("_layer_") }
                ?.toSet() ?: emptySet()

        val extraItemPNGs = itemFiles - configArmors
        val extraLayerPNGs = layerFiles - configArmors
        val missingItemPNGs = configArmors - itemFiles
        val missingLayerPNGs = configArmors - layerFiles

        val customModelDataMap = mutableMapOf<Int, MutableList<String>>()
        configArmors.forEach { armorName ->
            val customModelData = armorsConfig.getInt("armors.$armorName.custom_model_data")
            customModelDataMap.getOrPut(customModelData) { mutableListOf() }.add(armorName)
        }

        val duplicateCustomModelData = customModelDataMap.filter { it.value.size > 1 }

        if (extraItemPNGs.isNotEmpty() || extraLayerPNGs.isNotEmpty() || missingItemPNGs.isNotEmpty() || missingLayerPNGs.isNotEmpty() || duplicateCustomModelData.isNotEmpty()) {
            plugin.logger.warning("\u001B[33m+--------------------------+\u001B[0m")
            plugin.logger.warning("\u001B[33m| Resource Pack Configuration Warnings |\u001B[0m")
            plugin.logger.warning("\u001B[33m+--------------------------+\u001B[0m")

            if (extraItemPNGs.isNotEmpty()) {
                plugin.logger.warning("\u001B[31m| Armors with PNG files in item_files but not defined in config:\u001B[0m")
                extraItemPNGs.forEach { armorName ->
                    plugin.logger.warning("\u001B[31m| - $armorName\u001B[0m")
                }
            }

            if (extraLayerPNGs.isNotEmpty()) {
                plugin.logger.warning("\u001B[31m| Armors with PNG files in layer_files but not defined in config:\u001B[0m")
                extraLayerPNGs.forEach { armorName ->
                    plugin.logger.warning("\u001B[31m| - $armorName\u001B[0m")
                }
            }

            if (missingItemPNGs.isNotEmpty()) {
                plugin.logger.warning("\u001B[31m| Armors defined in config but missing PNG files in item_files:\u001B[0m")
                missingItemPNGs.forEach { armorName ->
                    plugin.logger.warning("\u001B[31m| - $armorName\u001B[0m")
                }
            }

            if (missingLayerPNGs.isNotEmpty()) {
                plugin.logger.warning("\u001B[31m| Armors defined in config but missing PNG files in layer_files:\u001B[0m")
                missingLayerPNGs.forEach { armorName ->
                    plugin.logger.warning("\u001B[31m| - $armorName\u001B[0m")
                }
            }

            if (duplicateCustomModelData.isNotEmpty()) {
                plugin.logger.warning("\u001B[31m| Duplicate Custom Model Data found:\u001B[0m")
                duplicateCustomModelData.forEach { (customModelData, armors) ->
                    plugin.logger.warning("\u001B[31m| Custom Model Data $customModelData is used by:\u001B[0m")
                    armors.forEach { armorName ->
                        plugin.logger.warning("\u001B[31m| - $armorName\u001B[0m")
                    }
                }
            }

            plugin.logger.warning("\u001B[33m+--------------------------+\u001B[0m")
            plugin.logger.warning("\u001B[33m| Please review your config, source folders, and Custom Model Data assignments.\u001B[0m")
            plugin.logger.warning("\u001B[33m+--------------------------+\u001B[0m")
        }
    }
}