package dev.furq.armament.commands

import dev.furq.armament.Armament
import dev.furq.armament.utils.ArmorCreator
import dev.furq.armament.utils.ArmorGUI
import dev.furq.armament.utils.ResourcePackGenerator
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File

class ArmamentCommand(private val plugin: Armament) : CommandExecutor {

    private val prefix = plugin.getMessage("prefix")
    private val armorCreator = ArmorCreator(plugin)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.label.equals("armament", ignoreCase = true) && args.isNotEmpty()) {
            when (args[0].lowercase()) {
                "reload" -> handleReload(sender)
                "give" -> handleGive(sender, args)
                "giveset" -> handleGiveSet(sender, args)
                "gui" -> handleGUI(sender)
                else -> sender.sendMessage("$prefix ${plugin.getMessage("command-unknown")}")
            }
        }
        return true
    }

    private fun handleReload(sender: CommandSender) {
        plugin.reloadConfig()
        plugin.loadConfigs()
        listOf("source_files", "resource_pack").forEach {
            File(plugin.dataFolder, it).mkdirs()
        }
        ResourcePackGenerator(plugin).generateResourcePack(
            File(plugin.dataFolder, "source_files"),
            File(plugin.dataFolder, "resource_pack")
        )
        sender.sendMessage("$prefix ${plugin.getMessage("reload-success")}")
    }

    private fun handleGive(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage("$prefix §7Usage: /armament give <armorName> <armorPiece> [player]")
            return
        }
        val armorName = args[1]
        val armorPiece = args[2]
        val targetPlayer = if (args.size >= 4) Bukkit.getPlayer(args[3]) else sender as? Player
        val armorsCofig = plugin.getArmorsConfig()
        val armors = armorsCofig.getConfigurationSection("armors")
        if (armorName !in armors?.getKeys(false).orEmpty()) {
            sender.sendMessage("$prefix ${plugin.getMessage("armor-not-found")}")
            return
        }

        val armorItem = armorCreator.createArmorPiece(armorName, armorPiece) ?: return

        if (targetPlayer != null) {
            if (targetPlayer.inventory.firstEmpty() == -1) {
                targetPlayer.world.dropItemNaturally(targetPlayer.location, armorItem)
            } else {
                targetPlayer.inventory.addItem(armorItem)
            }
            val armorPieceName = ChatColor.translateAlternateColorCodes(
                '&',
                armors?.getString("$armorName.$armorPiece.name") ?: armorName.replaceFirstChar { it.uppercase() })
            sender.sendMessage(
                "$prefix ${
                    plugin.getMessage("armor-given").replace("{player}", targetPlayer.name)
                        .replace("{armorName}", armorPieceName)
                }"
            )
            if (sender != targetPlayer) {
                targetPlayer.sendMessage(
                    "$prefix ${
                        plugin.getMessage("armor-received").replace(
                            "{armorName}",
                            armorPieceName
                        )
                    }"
                )
            }
        } else {
            sender.sendMessage("$prefix ${plugin.getMessage("player-not-found")}")
        }
    }

    private fun handleGiveSet(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("$prefix §7Usage: /armament giveset <armorName> [player]")
            return
        }
        val armorName = args[1]
        val armorsConfig = plugin.getArmorsConfig()
        val armors = armorsConfig.getConfigurationSection("armors")?.getKeys(false)
            ?: return sender.sendMessage("$prefix ${plugin.getMessage("armors-not-found")}")

        if (armorName !in armors) {
            sender.sendMessage("$prefix ${plugin.getMessage("armor-not-found")}")
            return
        }
        val armorItems = armorCreator.createFullArmorSet(armorName)

        val targetPlayer: Player? = when {
            args.size >= 3 -> Bukkit.getPlayer(args[2])
            sender is Player -> sender
            else -> null
        }

        if (targetPlayer != null) {
            armorItems.forEach {
                if (targetPlayer.inventory.firstEmpty() == -1) {
                    it?.let { piece -> targetPlayer.world.dropItemNaturally(targetPlayer.location, piece) }
                } else {
                    targetPlayer.inventory.addItem(it)
                }
            }
            sender.sendMessage(
                "$prefix ${
                    plugin.getMessage("armorset-given").replace("{player}", targetPlayer.name)
                        .replace("{armorName}", armorName.replaceFirstChar { it.uppercase() })
                }"
            )
            targetPlayer.sendMessage(
                "$prefix ${
                    plugin.getMessage("armorset-received")
                        .replace("{armorName}", armorName.replaceFirstChar { it.uppercase() })
                }"
            )
        } else {
            sender.sendMessage("$prefix ${plugin.getMessage("player-not-found")}")
        }
    }

    private fun handleGUI(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("$prefix ${plugin.getMessage("player-only-command")}")
            return
        }
        ArmorGUI(plugin).openGUI(sender, 0)
    }
}