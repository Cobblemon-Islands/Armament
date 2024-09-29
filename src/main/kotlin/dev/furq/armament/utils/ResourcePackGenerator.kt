package dev.furq.armament.utils

import dev.furq.armament.Armament
import org.bukkit.configuration.file.FileConfiguration
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ResourcePackGenerator(private val plugin: Armament) {
    private val materialGetter = MaterialGetter(plugin)

    fun generateResourcePack(sourceFolder: File, targetFolder: File) {
        val armorsConfig = plugin.getArmorsConfig()
        File(sourceFolder, "item_files").mkdirs()
        File(sourceFolder, "layer_files").mkdirs()

        cleanupResourcePack(targetFolder, armorsConfig)
        validateResourcePackConfiguration(armorsConfig, sourceFolder)
        val modelsItemDir = File(targetFolder, "assets/minecraft/models/item").apply { mkdirs() }
        val modelsArmorDir = File(targetFolder, "assets/minecraft/models/armor").apply { mkdirs() }
        val texturesItemArmorDir = File(targetFolder, "assets/minecraft/textures/item/armors").apply { mkdirs() }
        val trimsDir = File(targetFolder, "assets/armament/textures/trims/models/armor").apply { mkdirs() }
        val atlasesDir = File(targetFolder, "assets/minecraft/atlases").apply { mkdirs() }
        val modelsArmorTexturesDir = File(targetFolder, "assets/minecraft/textures/models/armor").apply { mkdirs() }
        copyResourceFolder("resource_pack/minecraft/textures/models/armor", modelsArmorTexturesDir)

        val material = materialGetter.getArmorString().lowercase()
        copyTrimFiles(material, targetFolder)

        armorsConfig.getConfigurationSection("armors")?.getKeys(false)?.forEach { armorName ->
            val armorSection = armorsConfig.getConfigurationSection("armors.$armorName")
            listOf("helmet", "chestplate", "leggings", "boots").forEach { piece ->
                if (armorSection?.contains(piece) == true) {
                    copyFile(
                        File(sourceFolder, "item_files/${armorName}_$piece.png"),
                        File(texturesItemArmorDir, "${armorName}_$piece.png")
                    )
                    
                    val modelData = mapOf(
                        "parent" to "item/generated",
                        "textures" to mapOf(
                            "layer0" to "item/armors/${armorName}_$piece",
                            "layer1" to "item/armors/${armorName}_$piece"
                        )
                    )
                    File(modelsArmorDir, "${armorName}_$piece.json").writeText(modelData.toJson())
                }
            }

            copyFile(File(sourceFolder, "layer_files/${armorName}_layer_1.png"), File(trimsDir, "$armorName.png"))
            copyFile(File(sourceFolder, "layer_files/${armorName}_layer_2.png"), File(trimsDir, "${armorName}_leggings.png"))
        }

        listOf("helmet", "chestplate", "leggings", "boots").forEach { piece ->
            val overrides = armorsConfig.getConfigurationSection("armors")?.getKeys(false)?.mapNotNull { armorName ->
                val customModelData = armorsConfig.getInt("armors.$armorName.custom_model_data")
                mapOf(
                    "predicate" to mapOf("custom_model_data" to customModelData),
                    "model" to "armor/${armorName}_$piece"
                )
            } ?: emptyList()

            val materialData = mapOf(
                "parent" to "item/generated",
                "textures" to mapOf(
                    "layer0" to "minecraft:item/${material}_$piece"
                ),
                "overrides" to overrides
            )
            File(modelsItemDir, "${material}_$piece.json").writeText(materialData.toJson())
        }

        val armorTrimsData = generateArmorTrimsJson(armorsConfig)
        File(atlasesDir, "armor_trims.json").writeText(armorTrimsData.toJson())

        val blocksData = generateBlocksJson(armorsConfig)
        File(atlasesDir, "blocks.json").writeText(blocksData.toJson())

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
        val textures = mutableListOf<String>()
        armorsConfig.getConfigurationSection("armors")?.getKeys(false)?.forEach { armorName ->
            textures.add("armament:trims/models/armor/$armorName")
            textures.add("armament:trims/models/armor/${armorName}_leggings")
        }
        textures.addAll(
            listOf(
                "minecraft:trims/models/armor/diamond",
                "minecraft:trims/models/armor/diamond_leggings"
            )
        )

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
                    "textures" to textures
                )
            )
        )
    }

    private fun generateBlocksJson(armorsConfig: FileConfiguration): Map<String, Any> {
        val sources = mutableListOf<Map<String, Any>>()

        armorsConfig.getConfigurationSection("armors")?.getKeys(false)?.forEach { armorName ->
            sources.addAll(
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
            )
        }

        sources.addAll(
            listOf(
                mapOf(
                    "type" to "single",
                    "resource" to "minecraft:trims/models/armor/diamond_leggings",
                    "sprite" to "minecraft:trims/models/armor/diamond_leggings"
                ),
                mapOf(
                    "type" to "single",
                    "resource" to "minecraft:trims/models/armor/diamond",
                    "sprite" to "minecraft:trims/models/armor/diamond"
                ),
                mapOf(
                    "type" to "single",
                    "resource" to "minecraft:models/armor/leather_layer_2_overlay",
                    "sprite" to "minecraft:models/armor/leather_layer_2_overlay"
                ),
                mapOf(
                    "type" to "single",
                    "resource" to "minecraft:models/armor/leather_layer_1_overlay",
                    "sprite" to "minecraft:models/armor/leather_layer_1_overlay"
                )
            )
        )

        return mapOf("sources" to sources)
    }

    private fun copyFile(source: File, destination: File) {
        if (source.exists()) {
            source.copyTo(destination, overwrite = true)
        }
    }

    private fun copyResourceFolder(resourcePath: String, targetDir: File) {
        val resourceFiles = listOf(
            "chainmail_layer_1.png",
            "chainmail_layer_2.png",
            "diamond_layer_1.png",
            "diamond_layer_2.png",
            "gold_layer_1.png",
            "gold_layer_2.png",
            "iron_layer_1.png",
            "iron_layer_2.png",
            "netherite_layer_1.png",
            "netherite_layer_2.png",
        )

        resourceFiles.forEach { fileName ->
            plugin.getResource("$resourcePath/$fileName")?.let { inputStream ->
                val targetFile = File(targetDir, fileName)
                targetFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }

    private fun copyTrimFiles(material: String, targetFolder: File) {
        val sourceDir = File(targetFolder, "assets/minecraft/textures/models/armor")
        val targetDir = File(targetFolder, "assets/minecraft/textures/trims/models/armor")
        targetDir.mkdirs()

        val layerFiles = listOf(
            "${material}_layer_1.png" to "$material.png",
            "${material}_layer_2.png" to "${material}_leggings.png"
        )

        layerFiles.forEach { (sourceName, targetName) ->
            val sourceFile = File(sourceDir, sourceName)
            val targetFile = File(targetDir, targetName)
            if (sourceFile.exists()) {
                sourceFile.copyTo(targetFile, overwrite = true)
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

    private fun Map<String, Any>.toJson(indent: String = ""): String = buildString {
        append("$indent{\n")
        this@toJson.entries.joinToString(",\n") { (key, value) ->
            "$indent  \"$key\": ${value.toJson("$indent  ")}"
        }.also { append(it) }
        append("\n$indent}")
    }

    private fun List<*>.toJson(indent: String = ""): String = buildString {
        append("$indent[\n")
        this@toJson.joinToString(",\n") { item ->
            "$indent  ${item.toJson("$indent  ")}"
        }.also { append(it) }
        append("\n$indent]")
    }

    private fun Any?.toJson(indent: String = ""): String = when (this) {
        is String -> "\"$this\""
        is Number, is Boolean -> this.toString()
        is Map<*, *> -> (this as Map<String, Any>).toJson(indent)
        is List<*> -> this.toJson(indent)
        else -> "\"$this\""
    }

    private fun cleanupResourcePack(targetFolder: File, armorsConfig: FileConfiguration) {
        val currentArmors = armorsConfig.getConfigurationSection("armors")?.getKeys(false) ?: emptySet()
        val texturesItemArmorDir = File(targetFolder, "assets/minecraft/textures/item/armors")
        texturesItemArmorDir.listFiles()?.forEach { file ->
            val armorName = file.nameWithoutExtension.substringBeforeLast("_")
            if (armorName !in currentArmors) {
                file.delete()
            }
        }
        val modelsArmorDir = File(targetFolder, "assets/minecraft/models/armor")
        modelsArmorDir.listFiles()?.forEach { file ->
            val armorName = file.nameWithoutExtension.substringBeforeLast("_")
            if (armorName !in currentArmors) {
                file.delete()
            }
        }
        val trimsDir = File(targetFolder, "assets/armament/textures/trims/models/armor")
        trimsDir.listFiles()?.forEach { file ->
            val armorName = file.nameWithoutExtension.removeSuffix("_leggings")
            if (armorName !in currentArmors) {
                file.delete()
            }
        }
    }

    private fun validateResourcePackConfiguration(armorsConfig: FileConfiguration, sourceFolder: File) {
        val configArmors = armorsConfig.getConfigurationSection("armors")?.getKeys(false) ?: emptySet()
        
        val itemFiles = File(sourceFolder, "item_files").listFiles { file -> file.extension.equals("png", ignoreCase = true) }
            ?.map { it.nameWithoutExtension.substringBeforeLast("_") }
            ?.toSet() ?: emptySet()
        
        val layerFiles = File(sourceFolder, "layer_files").listFiles { file -> file.extension.equals("png", ignoreCase = true) }
            ?.map { it.nameWithoutExtension.substringBeforeLast("_layer_") }
            ?.toSet() ?: emptySet()
        
        val sourcePNGs = (itemFiles + layerFiles).toSet()
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
            plugin.logger.warning("=== Resource Pack Configuration Warnings ===")
            
            if (extraItemPNGs.isNotEmpty()) {
                plugin.logger.warning("Armors with PNG files in item_files but not defined in config:")
                extraItemPNGs.forEach { armorName ->
                    plugin.logger.warning("  - $armorName")
                }
            }
            
            if (extraLayerPNGs.isNotEmpty()) {
                plugin.logger.warning("Armors with PNG files in layer_files but not defined in config:")
                extraLayerPNGs.forEach { armorName ->
                    plugin.logger.warning("  - $armorName")
                }
            }
            
            if (missingItemPNGs.isNotEmpty()) {
                plugin.logger.warning("Armors defined in config but missing PNG files in item_files:")
                missingItemPNGs.forEach { armorName ->
                    plugin.logger.warning("  - $armorName")
                }
            }
            
            if (missingLayerPNGs.isNotEmpty()) {
                plugin.logger.warning("Armors defined in config but missing PNG files in layer_files:")
                missingLayerPNGs.forEach { armorName ->
                    plugin.logger.warning("  - $armorName")
                }
            }
            
            if (duplicateCustomModelData.isNotEmpty()) {
                plugin.logger.warning("Duplicate Custom Model Data found:")
                duplicateCustomModelData.forEach { (customModelData, armors) ->
                    plugin.logger.warning("  Custom Model Data $customModelData is used by:")
                    armors.forEach { armorName ->
                        plugin.logger.warning("    - $armorName")
                    }
                }
            }
            
            plugin.logger.warning("Please review your config, source folders, and Custom Model Data assignments.")
        }
    }
}