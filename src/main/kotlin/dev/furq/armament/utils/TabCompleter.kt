package dev.furq.armament.utils

import dev.furq.armament.Armament
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class TabCompleter(private val plugin: Armament) : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>,
    ): List<String>? {
        if (command.name.equals("armament", ignoreCase = true)) {
            val armorsConfig = plugin.getArmorsConfig()
            when (args.size) {
                1 -> {
                    return listOf("reload", "give", "giveset", "gui")
                }
                2 -> {
                    when (args[0].lowercase()) {
                        "give", "giveset" -> {
                            val armors = armorsConfig.getConfigurationSection("armors")?.getKeys(false) ?: return null
                            return armors.toList()
                        }

                        else -> return null
                    }
                }
                3 -> {
                    when (args[0].lowercase()) {
                        "give" -> {
                            val selectedArmor = armorsConfig.getConfigurationSection("armors.${args[1]}")
                            return listOf("helmet", "chestplate", "leggings", "boots")
                                .filter { piece -> selectedArmor?.contains(piece) == true }
                        }

                        else -> return null
                    }
                }
            }
        }
        return null
    }
}