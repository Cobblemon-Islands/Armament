package dev.furq.armament.utils

import dev.furq.armament.Armament
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class TabCompleter(private val plugin: Armament) : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>,
    ): List<String>? {
        if (command.name.equals("armament", ignoreCase = true)) {
            if (args.size == 1) {
                return listOf("reload", "give", "giveset", "gui", "giveshoes")
            } else if (args.size == 2) {
                when (args[0].lowercase()) {
                    "give" -> {
                        val armorsConfigFile = File(plugin.dataFolder, "armors.yml")
                        if (!armorsConfigFile.exists()) plugin.saveResource("armors.yml", false)
                        val armorsConfig = YamlConfiguration.loadConfiguration(armorsConfigFile)
                        val armors = armorsConfig.getConfigurationSection("armors")?.getKeys(false)!!
                        return armors.toList()
                    }
                    "giveset" -> {
                        val armorsConfigFile = File(plugin.dataFolder, "armors.yml")
                        if (!armorsConfigFile.exists()) plugin.saveResource("armors.yml", false)
                        val armorsConfig = YamlConfiguration.loadConfiguration(armorsConfigFile)
                        val armors = armorsConfig.getConfigurationSection("armors")?.getKeys(false)!!
                        return armors.toList()
                    }
                    "giveshoes" -> {
                        return listOf("slow","medium","fast")
                    }
                    else -> return null
                }
            } else if (args.size == 3) {
                when (args[0].lowercase()) {
                    "give" -> {
                        val armorsConfigFile = File(plugin.dataFolder, "armors.yml")
                        if (!armorsConfigFile.exists()) plugin.saveResource("armors.yml", false)
                        val armorsConfig = YamlConfiguration.loadConfiguration(armorsConfigFile)
                        val selectedArmor = armorsConfig.getConfigurationSection("armors.${args[1]}")
                        
                        return listOf("helmet", "chestplate", "leggings", "boots")
                            .filter { piece -> selectedArmor?.contains(piece) == true }
                    }
                    else -> return null
                }
            }
        }
        return null
    }
}